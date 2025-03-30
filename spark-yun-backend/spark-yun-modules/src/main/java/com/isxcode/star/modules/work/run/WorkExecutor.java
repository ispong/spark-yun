package com.isxcode.star.modules.work.run;


import com.isxcode.star.api.alarm.constants.AlarmEventType;
import com.isxcode.star.api.instance.constants.InstanceStatus;
import com.isxcode.star.api.instance.constants.InstanceType;
import com.isxcode.star.api.work.constants.EventType;
import com.isxcode.star.api.work.constants.WorkLog;
import com.isxcode.star.backend.api.base.exceptions.WorkRunException;
import com.isxcode.star.common.locker.Locker;
import com.isxcode.star.modules.alarm.service.AlarmService;
import com.isxcode.star.modules.work.entity.*;
import com.isxcode.star.modules.work.repository.*;
import com.isxcode.star.modules.work.sql.SqlFunctionService;
import com.isxcode.star.modules.workflow.entity.WorkflowInstanceEntity;
import com.isxcode.star.modules.workflow.repository.WorkflowInstanceRepository;

import java.time.LocalDateTime;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;

@Slf4j
@RequiredArgsConstructor
public abstract class WorkExecutor {

    public static final Map<String, Thread> WORK_THREAD = new HashMap<>();

    private final AlarmService alarmService;

    private final Scheduler scheduler;

    private final Locker locker;

    private final WorkRepository workRepository;

    private final WorkInstanceRepository workInstanceRepository;

    private final WorkflowInstanceRepository workflowInstanceRepository;

    private final WorkEventRepository workEventRepository;

    private final WorkRunJobFactory workRunJobFactory;

    private final SqlFunctionService sqlFunctionService;

    private final WorkConfigRepository workConfigRepository;

    private final VipWorkVersionRepository vipWorkVersionRepository;

    public abstract String getWorkType();

    protected abstract String execute(WorkRunContext workRunContext, WorkInstanceEntity workInstance);

    protected abstract void abort(WorkInstanceEntity workInstance);

    /**
     * 判断当前进程是否运行过
     */
    public boolean processNeverRun(WorkEventEntity workEvent, Integer nowIndex) {

        // 上一步状态保存
        if (workEvent.getEventProcess() + 1 == nowIndex) {
            workEvent.setEventProcess(nowIndex);
            workEventRepository.saveAndFlush(workEvent);
        }

        // 返回是否执行过
        return workEvent.getEventProcess() < nowIndex + 1;
    }

    /**
     * 翻译上游的jsonPath.
     */
    public String parseJsonPath(String value, WorkInstanceEntity workInstance) {

        if (workInstance.getWorkflowInstanceId() == null) {
            return value.replace("get_json_value", "get_json_default_value")
                .replace("get_regex_value", "get_regex_default_value")
                .replace("get_table_value", "get_table_default_value");
        }

        List<WorkInstanceEntity> allWorkflowInstance =
            workInstanceRepository.findAllByWorkflowInstanceId(workInstance.getWorkflowInstanceId());

        for (WorkInstanceEntity e : allWorkflowInstance) {
            if (InstanceStatus.SUCCESS.equals(e.getStatus()) && e.getResultData() != null) {
                value = value.replace("${qing." + e.getWorkId() + ".result_data}",
                    Base64.getEncoder().encodeToString(e.getResultData().getBytes()));
            }
        }

        return sqlFunctionService.parseSqlFunction(value);
    }

    public WorkInstanceEntity updateInstance(WorkInstanceEntity workInstance, StringBuilder logBuilder) {

        workInstance.setSubmitLog(logBuilder.toString());
        return workInstanceRepository.saveAndFlush(workInstance);
    }

    /**
     * 关闭定时器.
     */
    public void deleteEventAndQuartz(String eventId) {

        // 删除事件
        workEventRepository.deleteByIdAndFlush(eventId);

        // 删除定时器
        try {
            scheduler.unscheduleJob(TriggerKey.triggerKey("event_" + eventId));
        } catch (SchedulerException e) {
            log.error(e.getMessage());
        }

    }

    public void runWork(WorkRunContext workRunContext) {

        // 获取最新的作业事件
        WorkEventEntity workEvent = workEventRepository.findById(workRunContext.getEventId()).get();

        // 获取最新的作业实例
        WorkInstanceEntity workInstance = workInstanceRepository.findById(workRunContext.getInstanceId()).get();

        // 修改作业的状态为运行中
        if (processNeverRun(workEvent, 1)) {

            if (EventType.WORKFLOW.equals(workRunContext.getEventType())) {

                // 如果是作业流中的作业，特殊处理
                // 修改状态前先加锁，给工作流实例加锁
                Integer lockId = locker.lock(workRunContext.getFlowInstanceId());

                // 已中止的任务，不可以再运行
                if (InstanceStatus.ABORT.equals(workInstance.getStatus())) {
                    deleteEventAndQuartz(workEvent.getId());
                    return;
                }

                // 跑过了或者正在跑的，不可以再运行
                if (!InstanceStatus.PENDING.equals(workInstance.getStatus())
                    && !InstanceStatus.BREAK.equals(workInstance.getStatus())) {
                    deleteEventAndQuartz(workEvent.getId());
                    return;
                }

                // 在调度中，如果自身定时器没有被触发，不可以再运行
                if (!Strings.isEmpty(workRunContext.getVersionId()) && !workInstance.getQuartzHasRun()) {
                    deleteEventAndQuartz(workEvent.getId());
                    return;
                }

                // 获取父级别的作业状态
                List<String> parentNodes =
                    WorkUtils.getParentNodes(workRunContext.getNodeMapping(), workRunContext.getWorkId());
                List<WorkInstanceEntity> parentInstances = workInstanceRepository
                    .findAllByWorkIdAndWorkflowInstanceId(parentNodes, workRunContext.getFlowInstanceId());
                boolean parentIsError =
                    parentInstances.stream().anyMatch(e -> InstanceStatus.FAIL.equals(e.getStatus()));
                boolean parentIsBreak =
                    parentInstances.stream().anyMatch(e -> InstanceStatus.BREAK.equals(e.getStatus()));
                boolean parentIsRunning = parentInstances.stream().anyMatch(
                    e -> InstanceStatus.RUNNING.equals(e.getStatus()) || InstanceStatus.PENDING.equals(e.getStatus()));

                // 如果父级在运行中，不可以再运行
                if (parentIsRunning) {
                    deleteEventAndQuartz(workEvent.getId());
                    return;
                }

                if (parentIsError) {
                    // 如果父级有错，则状态直接变更为失败
                    workInstance.setStatus(InstanceStatus.FAIL);
                    workInstance.setSubmitLog("父级执行失败");
                    workInstance.setExecStartDateTime(new Date());
                    workInstance.setExecEndDateTime(new Date());
                    workInstance.setDuration(0L);
                } else if (parentIsBreak || InstanceStatus.BREAK.equals(workInstance.getStatus())) {
                    // 如果父级有中断，则状态直接变更为中断
                    workInstance.setStatus(InstanceStatus.BREAK);
                    workInstance.setExecEndDateTime(new Date());
                    workInstance.setDuration(
                        (System.currentTimeMillis() - workInstance.getExecStartDateTime().getTime()) / 1000);
                } else {
                    // 要立马变成RUNNING状态，防止别的并发修改状态
                    workInstance.setSubmitLog(LocalDateTime.now() + WorkLog.SUCCESS_INFO + "开始提交作业 \n");
                    workInstance.setStatus(InstanceStatus.RUNNING);
                    workInstance.setExecStartDateTime(new Date());
                }

                // 保存作业状态并解锁
                workInstanceRepository.saveAndFlush(workInstance);
                locker.unlock(lockId);
            } else {

                // 单个作业状态改成RUNNING
                workInstance.setSubmitLog(LocalDateTime.now() + WorkLog.SUCCESS_INFO + "开始提交作业 \n");
                workInstance.setStatus(InstanceStatus.RUNNING);
                workInstance.setExecStartDateTime(new Date());
                workInstanceRepository.saveAndFlush(workInstance);
            }
        }

        // 基线管理，任务开始运行发送消息
        if (processNeverRun(workEvent, 2)) {
            if (InstanceType.AUTO.equals(workInstance.getInstanceType())) {
                alarmService.sendWorkMessage(workInstance, AlarmEventType.START_RUN);
            }
        }

        // 开始运行作业
        String executeStatus;
        try {

            // 开始执行作业，每次都要执行
            executeStatus = execute(workRunContext, workInstance);

            // 判断任务是否执行完
            if (InstanceStatus.SUCCESS.equals(executeStatus)) {

                // 没有报错，默认作业已经运行成功，获取最新作业实例
                workInstance = workInstanceRepository.findById(workRunContext.getInstanceId()).get();
                workInstance.setStatus(InstanceStatus.SUCCESS);
                workInstance.setExecEndDateTime(new Date());
                workInstance
                    .setDuration((System.currentTimeMillis() - workInstance.getExecStartDateTime().getTime()) / 1000);
                workInstance
                    .setSubmitLog(workInstance.getSubmitLog() + LocalDateTime.now() + WorkLog.SUCCESS_INFO + "执行成功 \n");
                workInstanceRepository.save(workInstance);

                // 基线管理，任务运行成功发送消息
                if (InstanceType.AUTO.equals(workInstance.getInstanceType())) {
                    alarmService.sendWorkMessage(workInstance, AlarmEventType.RUN_SUCCESS);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            // 获取异常日志
            String msg = e instanceof WorkRunException ? ((WorkRunException) e).getMsg() : e.getMessage();

            // 重新获取当前最新实例
            workInstance = workInstanceRepository.findById(workRunContext.getInstanceId()).get();

            // 如果是已中止，不可以再运行
            if (InstanceStatus.ABORT.equals(workInstance.getStatus())) {
                deleteEventAndQuartz(workEvent.getId());
                return;
            }

            // 更新作业实例失败状态
            workInstance.setStatus(InstanceStatus.FAIL);
            workInstance.setExecEndDateTime(new Date());
            workInstance
                .setDuration((System.currentTimeMillis() - workInstance.getExecStartDateTime().getTime()) / 1000);
            workInstance
                .setSubmitLog(workInstance.getSubmitLog() + msg + LocalDateTime.now() + WorkLog.ERROR_INFO + "执行失败 \n");
            workInstanceRepository.save(workInstance);

            // 基线管理，任务运行失败发送消息
            if (InstanceType.AUTO.equals(workInstance.getInstanceType())) {
                alarmService.sendWorkMessage(workInstance, AlarmEventType.RUN_FAIL);
            }

            executeStatus = InstanceStatus.FAIL;
        }

        // 当前作业是否运行结束
        if (InstanceStatus.FAIL.equals(executeStatus) || InstanceStatus.SUCCESS.equals(executeStatus)) {

            // 基线管理，任务运行结束发送消息
            if (InstanceType.AUTO.equals(workInstance.getInstanceType())) {
                alarmService.sendWorkMessage(workInstance, AlarmEventType.RUN_END);
            }

            // 删除事件
            workEventRepository.deleteByIdAndFlush(workEvent.getId());

            // 删除定时器
            try {
                scheduler.unscheduleJob(TriggerKey.triggerKey("event_" + workInstance.getId()));
            } catch (SchedulerException e) {
                log.error(e.getMessage());
            }

            // 移除任务进程
            WORK_THREAD.remove(workInstance.getId());

            // 如果是作业流的任务，则需要继续推送
            if (EventType.WORKFLOW.equals(workRunContext.getEventType())) {

                // 加锁，修改作业流状态
                Integer lockId = locker.lock(workRunContext.getFlowInstanceId());
                WorkflowInstanceEntity lastWorkflowInstance =
                    workflowInstanceRepository.findById(workRunContext.getFlowInstanceId()).get();

                // 中止中作业，不可以再运行
                if (InstanceStatus.ABORTING.equals(lastWorkflowInstance.getStatus())) {
                    deleteEventAndQuartz(workEvent.getId());
                    return;
                }

                // 获取结束节点实例状态，判断作业流是否执行完毕
                List<String> endNodes =
                    WorkUtils.getEndNodes(workRunContext.getNodeMapping(), workRunContext.getNodeList());
                List<WorkInstanceEntity> endNodeInstance = workInstanceRepository
                    .findAllByWorkIdAndWorkflowInstanceId(endNodes, workRunContext.getFlowInstanceId());
                boolean flowIsOver = endNodeInstance.stream()
                    .allMatch(e -> InstanceStatus.FAIL.equals(e.getStatus())
                        || InstanceStatus.SUCCESS.equals(e.getStatus()) || InstanceStatus.ABORT.equals(e.getStatus())
                        || InstanceStatus.BREAK.equals(e.getStatus()));

                // 如果作业流执行结束
                if (flowIsOver) {

                    // 修改作业流状态
                    boolean flowIsError =
                        endNodeInstance.stream().anyMatch(e -> InstanceStatus.FAIL.equals(e.getStatus()));
                    WorkflowInstanceEntity workflowInstance =
                        workflowInstanceRepository.findById(workRunContext.getFlowInstanceId()).get();
                    workflowInstance.setStatus(flowIsError ? InstanceStatus.FAIL : InstanceStatus.SUCCESS);
                    workflowInstance
                        .setRunLog(workflowInstanceRepository.getWorkflowLog(workRunContext.getFlowInstanceId()) + "\n"
                            + LocalDateTime.now() + (flowIsError ? WorkLog.ERROR_INFO : WorkLog.SUCCESS_INFO)
                            + (flowIsError ? "运行失败" : "运行成功"));
                    workflowInstance.setDuration(
                        (System.currentTimeMillis() - workflowInstance.getExecStartDateTime().getTime()) / 1000);
                    workflowInstance.setExecEndDateTime(new Date());

                    // 基线告警，作业流成功、失败、运行结束发送消息
                    if (InstanceType.AUTO.equals(workflowInstance.getInstanceType())) {
                        if (flowIsError) {
                            alarmService.sendWorkflowMessage(workflowInstance, AlarmEventType.RUN_FAIL);
                        } else {
                            alarmService.sendWorkflowMessage(workflowInstance, AlarmEventType.RUN_SUCCESS);
                        }
                        alarmService.sendWorkflowMessage(workflowInstance, AlarmEventType.RUN_END);
                    }

                    // 持久化作业流状态
                    workflowInstanceRepository.saveAndFlush(workflowInstance);
                } else {

                    // 工作流没有执行完，解析推送子节点
                    List<String> sonNodes =
                        WorkUtils.getSonNodes(workRunContext.getNodeMapping(), workRunContext.getWorkId());
                    List<WorkEntity> sonNodeWorks = workRepository.findAllByWorkIds(sonNodes);
                    sonNodeWorks.forEach(work -> {

                        // 查询作业的实例
                        WorkInstanceEntity sonWorkInstance = workInstanceRepository
                            .findByWorkIdAndWorkflowInstanceId(work.getId(), workRunContext.getFlowInstanceId());

                        // 封装WorkRunContext，通过是否有versionId，判断是调度中作业还是普通作业
                        WorkRunContext sonWorkRunContext;
                        if (Strings.isEmpty(sonWorkInstance.getVersionId())) {
                            WorkConfigEntity workConfig = workConfigRepository.findById(work.getConfigId()).get();
                            sonWorkRunContext = WorkUtils.genWorkRunContext(sonWorkInstance.getId(), EventType.WORKFLOW,
                                work, workConfig);
                        } else {
                            VipWorkVersionEntity workVersion =
                                vipWorkVersionRepository.findById(work.getVersionId()).get();
                            sonWorkRunContext = WorkUtils.genWorkRunContext(sonWorkInstance.getId(), EventType.WORKFLOW,
                                work, workVersion);
                            sonWorkRunContext.setVersionId(sonWorkInstance.getVersionId());
                        }
                        sonWorkRunContext.setDagEndList(workRunContext.getDagEndList());
                        sonWorkRunContext.setDagStartList(workRunContext.getDagStartList());
                        sonWorkRunContext.setFlowInstanceId(workRunContext.getFlowInstanceId());
                        sonWorkRunContext.setNodeMapping(workRunContext.getNodeMapping());
                        sonWorkRunContext.setNodeList(workRunContext.getNodeList());

                        // 调用定时器触发
                        workRunJobFactory.execute(sonWorkRunContext);
                    });
                }

                // 修改作业流状态，解锁
                locker.unlock(lockId);
            }
        }
    }

    public void syncAbort(WorkInstanceEntity workInstance) {

        this.abort(workInstance);
    }

}
