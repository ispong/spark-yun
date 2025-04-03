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
import org.springframework.dao.EmptyResultDataAccessException;

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

    protected abstract String execute(WorkRunContext workRunContext, WorkInstanceEntity workInstance,
        WorkEventEntity workEvent) throws Exception;

    protected abstract void abort(WorkInstanceEntity workInstance) throws Exception;

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

    public String runWork(WorkRunContext workRunContext) {

        if (EventType.WORK.equals(workRunContext.getEventType())) {
            return runSingleWork(workRunContext);
        } else {
            return runFlowWork(workRunContext);
        }
    }

    public void syncAbort(WorkInstanceEntity workInstance) throws Exception {

        this.abort(workInstance);
    }

    public String runSingleWork(WorkRunContext workRunContext) {

        // 获取事件
        WorkEventEntity workEvent = workEventRepository.findById(workRunContext.getEventId()).get();

        // 获取最新作业实例
        WorkInstanceEntity workInstance = workInstanceRepository.findById(workRunContext.getInstanceId()).get();

        // 中止、中止中，不可以再运行
        if (InstanceStatus.ABORT.equals(workInstance.getStatus())
            || InstanceStatus.ABORTING.equals(workInstance.getStatus())
            || InstanceStatus.SUCCESS.equals(workInstance.getStatus())
            || InstanceStatus.FAIL.equals(workInstance.getStatus())) {
            return InstanceStatus.FINISHED;
        }

        // 先将作业改成运行中，只有pending的作业才可以改成运行中
        if (InstanceStatus.PENDING.equals(workInstance.getStatus())) {

            // 单个作业实例状态改成运行中
            workInstance.setSubmitLog(LocalDateTime.now() + WorkLog.SUCCESS_INFO + "开始提交作业 \n");
            workInstance.setStatus(InstanceStatus.RUNNING);
            workInstance.setExecStartDateTime(new Date());
            workInstanceRepository.saveAndFlush(workInstance);
        }

        // 开始运行作业
        try {

            // 运行前，保存当前线程
            WORK_THREAD.put(workInstance.getId(), Thread.currentThread());

            // 开始执行作业，每次都要执行
            String executeStatus = execute(workRunContext, workInstance, workEvent);

            // 如果是运行中，直接跳过，下个调度再执行
            if (InstanceStatus.RUNNING.equals(executeStatus)) {
                return InstanceStatus.RUNNING;
            }

            // 作业运行成功
            if (InstanceStatus.SUCCESS.equals(executeStatus)) {

                // 只有运行中的作业，才能改成成功
                workInstance = workInstanceRepository.findById(workRunContext.getInstanceId()).get();
                if (InstanceStatus.RUNNING.equals(workInstance.getStatus())) {

                    // 运行中的修改为成功
                    workInstance.setStatus(InstanceStatus.SUCCESS);
                    workInstance.setExecEndDateTime(new Date());
                    workInstance.setDuration(
                        (System.currentTimeMillis() - workInstance.getExecStartDateTime().getTime()) / 1000);
                    workInstance.setSubmitLog(
                        workInstance.getSubmitLog() + LocalDateTime.now() + WorkLog.SUCCESS_INFO + "执行成功 \n");
                    workInstanceRepository.saveAndFlush(workInstance);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            // 获取异常日志
            String msg = e instanceof WorkRunException ? ((WorkRunException) e).getMsg() : e.getMessage();

            // 只有运行中的作业，才能改成失败
            workInstance = workInstanceRepository.findById(workRunContext.getInstanceId()).get();
            if (InstanceStatus.RUNNING.equals(workInstance.getStatus())) {
                workInstance.setStatus(InstanceStatus.FAIL);
                workInstance.setExecEndDateTime(new Date());
                workInstance
                    .setDuration((System.currentTimeMillis() - workInstance.getExecStartDateTime().getTime()) / 1000);
                workInstance.setSubmitLog(
                    workInstance.getSubmitLog() + msg + LocalDateTime.now() + WorkLog.ERROR_INFO + "执行失败 \n");
                workInstanceRepository.saveAndFlush(workInstance);
            }
        }

        // 单个作业运行结束
        WORK_THREAD.remove(workInstance.getId());
        return InstanceStatus.FINISHED;
    }

    public String runFlowWork(WorkRunContext workRunContext) {

        // 获取事件
        WorkEventEntity workEvent = workEventRepository.findById(workRunContext.getEventId()).get();

        // 获取最新作业实例
        WorkInstanceEntity workInstance = workInstanceRepository.findById(workRunContext.getInstanceId()).get();

        // 中止、中止中，不可以再运行
        if (InstanceStatus.ABORT.equals(workInstance.getStatus())
            || InstanceStatus.ABORTING.equals(workInstance.getStatus())) {
            return InstanceStatus.FINISHED;
        }

        // 一个作业的一个实例的一个eventId拿到锁，其他的都踢掉
        Integer lockId = locker.lockOnly("work_run_" + workRunContext.getInstanceId(), workRunContext.getEventId());
        if (locker.isLocked("work_run_" + workRunContext.getInstanceId(), workRunContext.getEventId())) {
            try {
                locker.unlock(lockId);
            } catch (EmptyResultDataAccessException e) {
                return InstanceStatus.FINISHED;
            }
            return InstanceStatus.FINISHED;
        }

        // 先将作业改成运行中，只有pending的作业才可以改成运行中
        if (InstanceStatus.PENDING.equals(workInstance.getStatus())) {

            // 基线管理，任务开始运行，发送消息
            if (InstanceType.AUTO.equals(workInstance.getInstanceType())) {
                alarmService.sendWorkMessage(workInstance, AlarmEventType.START_RUN);
            }

            // 在调度中的作业，如果自身定时器没有被触发，不可以再运行
            if (!Strings.isEmpty(workRunContext.getVersionId()) && !workInstance.getQuartzHasRun()) {
                locker.clearLock("work_run_" + workRunContext.getInstanceId());
                return InstanceStatus.FINISHED;
            }

            // 获取父级别的作业实例状态
            List<String> parentNodes =
                WorkUtils.getParentNodes(workRunContext.getNodeMapping(), workRunContext.getWorkId());
            List<WorkInstanceEntity> parentInstances = workInstanceRepository
                .findAllByWorkIdAndWorkflowInstanceId(parentNodes, workRunContext.getFlowInstanceId());
            boolean parentIsError = parentInstances.stream().anyMatch(e -> InstanceStatus.FAIL.equals(e.getStatus()));
            boolean parentIsBreak = parentInstances.stream().anyMatch(e -> InstanceStatus.BREAK.equals(e.getStatus()));
            boolean parentIsRunning = parentInstances.stream().anyMatch(
                e -> InstanceStatus.RUNNING.equals(e.getStatus()) || InstanceStatus.PENDING.equals(e.getStatus()));

            // 判断当前作业实例的状态
            if (parentIsRunning) {
                // 如果父级在运行中，不可以再运行
                locker.clearLock("work_run_" + workRunContext.getInstanceId());
                return InstanceStatus.FINISHED;
            } else if (parentIsError) {
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
                workInstance.setExecStartDateTime(new Date());
                workInstance
                    .setDuration((System.currentTimeMillis() - workInstance.getExecStartDateTime().getTime()) / 1000);
            } else {
                // 修改作业实例状态为运行中
                workInstance.setSubmitLog(LocalDateTime.now() + WorkLog.SUCCESS_INFO + "开始提交作业 \n");
                workInstance.setStatus(InstanceStatus.RUNNING);
                workInstance.setExecStartDateTime(new Date());
            }

            // 保存作业实例状态，解锁
            workInstanceRepository.saveAndFlush(workInstance);
        }

        // 实例只有running状态，才能执行作业
        if (InstanceStatus.RUNNING.equals(workInstance.getStatus())) {
            try {

                // 运行前，保存当前线程
                WORK_THREAD.put(workInstance.getId(), Thread.currentThread());

                // 开始执行作业，每次都要执行
                String executeStatus = execute(workRunContext, workInstance, workEvent);

                // 如果是运行中，直接跳过，下个调度再执行
                if (InstanceStatus.RUNNING.equals(executeStatus)) {
                    return InstanceStatus.RUNNING;
                }

                // 作业运行成功
                if (InstanceStatus.SUCCESS.equals(executeStatus)) {

                    // 只有运行中的作业，才能改成成功
                    workInstance = workInstanceRepository.findById(workRunContext.getInstanceId()).get();
                    if (InstanceStatus.RUNNING.equals(workInstance.getStatus())) {
                        // 运行中的修改为成功
                        workInstance.setStatus(InstanceStatus.SUCCESS);
                        workInstance.setExecEndDateTime(new Date());
                        workInstance.setDuration(
                            (System.currentTimeMillis() - workInstance.getExecStartDateTime().getTime()) / 1000);
                        workInstance.setSubmitLog(
                            workInstance.getSubmitLog() + LocalDateTime.now() + WorkLog.SUCCESS_INFO + "执行成功 \n");
                        workInstanceRepository.saveAndFlush(workInstance);

                        // 基线管理，任务运行成功发送消息
                        if (InstanceType.AUTO.equals(workInstance.getInstanceType())) {
                            alarmService.sendWorkMessage(workInstance, AlarmEventType.RUN_SUCCESS);
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);

                // 获取异常日志
                String msg = e instanceof WorkRunException ? ((WorkRunException) e).getMsg() : e.getMessage();

                // 只有运行中的作业，才能改成失败
                workInstance = workInstanceRepository.findById(workRunContext.getInstanceId()).get();
                if (InstanceStatus.RUNNING.equals(workInstance.getStatus())) {
                    workInstance.setStatus(InstanceStatus.FAIL);
                    workInstance.setExecEndDateTime(new Date());
                    workInstance.setDuration(
                        (System.currentTimeMillis() - workInstance.getExecStartDateTime().getTime()) / 1000);
                    workInstance.setSubmitLog(
                        workInstance.getSubmitLog() + msg + LocalDateTime.now() + WorkLog.ERROR_INFO + "执行失败 \n");
                    workInstanceRepository.saveAndFlush(workInstance);

                    // 基线管理，任务运行失败发送消息
                    if (InstanceType.AUTO.equals(workInstance.getInstanceType())) {
                        alarmService.sendWorkMessage(workInstance, AlarmEventType.RUN_FAIL);
                    }
                }
            }
            // 基线管理，任务运行结束发送消息
            if (processNeverRun(workEvent, 999)) {
                if (InstanceType.AUTO.equals(workInstance.getInstanceType())) {
                    alarmService.sendWorkMessage(workInstance, AlarmEventType.RUN_END);
                }
            }
        }

        // 如果是作业流的作业执行结束，需要继续推送任务
        if (InstanceStatus.FAIL.equals(workInstance.getStatus())
            || InstanceStatus.SUCCESS.equals(workInstance.getStatus())) {

            // 获取最新的作业流实例
            WorkflowInstanceEntity workflowInstance =
                workflowInstanceRepository.findById(workRunContext.getFlowInstanceId()).get();

            // 中止中的作业流，不可以再推送
            if (InstanceStatus.ABORTING.equals(workflowInstance.getStatus())) {
                locker.clearLock("work_run_" + workRunContext.getInstanceId());
                return InstanceStatus.FINISHED;
            }

            // 获取所有节点实例状态，判断作业流是否执行完毕
            List<WorkInstanceEntity> endNodeInstance;
            try {
                endNodeInstance = workInstanceRepository.findAllByWorkIdAndWorkflowInstanceId(
                    workRunContext.getNodeList(), workRunContext.getFlowInstanceId());
            } catch (Exception e) {
                return InstanceStatus.RUNNING;
            }

            boolean flowIsOver = endNodeInstance.stream()
                .allMatch(e -> InstanceStatus.FAIL.equals(e.getStatus()) || InstanceStatus.SUCCESS.equals(e.getStatus())
                    || InstanceStatus.ABORT.equals(e.getStatus()) || InstanceStatus.BREAK.equals(e.getStatus()));

            // 如果作业流执行结束
            if (flowIsOver) {

                // 修改作业流状态
                boolean flowIsError = endNodeInstance.stream().anyMatch(e -> InstanceStatus.FAIL.equals(e.getStatus()));
                workflowInstance.setDuration(
                    (System.currentTimeMillis() - workflowInstance.getExecStartDateTime().getTime()) / 1000);
                workflowInstance.setExecEndDateTime(new Date());
                if (flowIsError) {
                    workflowInstance.setStatus(InstanceStatus.FAIL);
                    workflowInstance
                        .setRunLog(workflowInstanceRepository.getWorkflowLog(workRunContext.getFlowInstanceId()) + "\n"
                            + LocalDateTime.now() + WorkLog.ERROR_INFO + "运行失败");
                } else {
                    workflowInstance.setStatus(InstanceStatus.SUCCESS);
                    workflowInstance
                        .setRunLog(workflowInstanceRepository.getWorkflowLog(workRunContext.getFlowInstanceId()) + "\n"
                            + LocalDateTime.now() + WorkLog.SUCCESS_INFO + "运行成功");
                }

                // 持久化作业流实例状态
                workflowInstanceRepository.saveAndFlush(workflowInstance);

                // 基线告警，作业流成功、失败、运行结束发送消息
                if (InstanceType.AUTO.equals(workflowInstance.getInstanceType())) {
                    if (flowIsError) {
                        alarmService.sendWorkflowMessage(workflowInstance, AlarmEventType.RUN_FAIL);
                    } else {
                        alarmService.sendWorkflowMessage(workflowInstance, AlarmEventType.RUN_SUCCESS);
                    }
                    alarmService.sendWorkflowMessage(workflowInstance, AlarmEventType.RUN_END);
                }
            }

            // 工作流没有执行完，继续推送子节点
            List<String> sonNodes = WorkUtils.getSonNodes(workRunContext.getNodeMapping(), workRunContext.getWorkId());
            List<WorkEntity> sonNodeWorks = workRepository.findAllByWorkIds(sonNodes);
            sonNodeWorks.forEach(work -> {

                // 查询子作业的实例
                WorkInstanceEntity sonWorkInstance = workInstanceRepository
                    .findByWorkIdAndWorkflowInstanceId(work.getId(), workRunContext.getFlowInstanceId());

                // 封装WorkRunContext，通过是否有versionId，判断是调度中作业还是普通手动运行的作业
                WorkRunContext sonWorkRunContext;
                if (Strings.isEmpty(sonWorkInstance.getVersionId())) {
                    WorkConfigEntity workConfig = workConfigRepository.findById(work.getConfigId()).get();
                    sonWorkRunContext =
                        WorkUtils.genWorkRunContext(sonWorkInstance.getId(), EventType.WORKFLOW, work, workConfig);
                } else {
                    VipWorkVersionEntity workVersion = vipWorkVersionRepository.findById(work.getVersionId()).get();
                    sonWorkRunContext =
                        WorkUtils.genWorkRunContext(sonWorkInstance.getId(), EventType.WORKFLOW, work, workVersion);
                    sonWorkRunContext.setVersionId(sonWorkInstance.getVersionId());
                }
                sonWorkRunContext.setDagEndList(workRunContext.getDagEndList());
                sonWorkRunContext.setDagStartList(workRunContext.getDagStartList());
                sonWorkRunContext.setFlowInstanceId(workRunContext.getFlowInstanceId());
                sonWorkRunContext.setNodeMapping(workRunContext.getNodeMapping());
                sonWorkRunContext.setNodeList(workRunContext.getNodeList());

                // 调用调度器触发子作业
                workRunJobFactory.execute(sonWorkRunContext);
            });

            // 作业运行结束，移除线程
            WORK_THREAD.remove(workInstance.getId());

            // 作业运行完，解锁
            locker.clearLock("work_run_" + workRunContext.getInstanceId());

            // 当前作业运行完毕
            return InstanceStatus.FINISHED;
        }

        // 正常继续执行
        return InstanceStatus.RUNNING;
    }
}
