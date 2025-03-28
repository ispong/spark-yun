package com.isxcode.star.modules.work.run;


import com.isxcode.star.api.alarm.constants.AlarmEventType;
import com.isxcode.star.api.instance.constants.InstanceStatus;
import com.isxcode.star.api.instance.constants.InstanceType;
import com.isxcode.star.api.work.constants.WorkLog;
import com.isxcode.star.backend.api.base.exceptions.WorkRunException;
import com.isxcode.star.common.locker.Locker;
import com.isxcode.star.modules.alarm.service.AlarmService;
import com.isxcode.star.modules.work.entity.*;
import com.isxcode.star.modules.work.repository.WorkEventRepository;
import com.isxcode.star.modules.work.repository.WorkInstanceRepository;
import com.isxcode.star.modules.workflow.entity.WorkflowInstanceEntity;
import com.isxcode.star.modules.workflow.repository.WorkflowInstanceRepository;

import java.time.LocalDateTime;
import java.util.*;

import com.isxcode.star.modules.workflow.run.WorkflowRunEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;

/**
 * 作业执行器.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class WorkExecutor {

    public static final Map<String, Thread> WORK_THREAD = new HashMap<>();

    private final WorkInstanceRepository workInstanceRepository;

    private final WorkflowInstanceRepository workflowInstanceRepository;

    private final AlarmService alarmService;

    private final WorkEventRepository workEventRepository;

    private final Scheduler scheduler;

    private final Locker locker;

    public abstract String getWorkType();

    protected abstract void execute(WorkRunContext workRunContext, WorkInstanceEntity workInstance);

    protected abstract void abort(WorkInstanceEntity workInstance);

    /**
     * 执行作业，核心逻辑
     */
    public void runWork(WorkRunContext workRunContext) {

        // 判断是作业运行，还是作业流运行
        if (!Strings.isEmpty(workRunContext.getFlowInstanceId())) {

            // 修改作业状态为RUNNING，但修改前都要加锁，给工作流实例加锁
            Integer lockId = locker.lock(workRunContext.getFlowInstanceId());

            // 查询作业实例
            WorkInstanceEntity workInstance = workInstanceRepository
                .findByWorkIdAndWorkflowInstanceId(workRunContext.getWorkId(), workRunContext.getFlowInstanceId());

            // 已中止的任务，不可以再跑
            if (InstanceStatus.ABORT.equals(workInstance.getStatus())) {
                return;
            }

            // 跑过了或者正在跑，不可以再跑
            if (!InstanceStatus.PENDING.equals(workInstance.getStatus())
                && !InstanceStatus.BREAK.equals(workInstance.getStatus())) {
                return;
            }

            // 在调度中，如果自身定时器没有被触发，不可以跑，先接受定时器触发，才能接受spring的event事件触发
            if (!Strings.isEmpty(workRunContext.getVersionId()) && !workInstance.getQuartzHasRun()) {
                return;
            }

            // 获取父级别的作业状态
            List<String> parentNodes =
                WorkUtils.getParentNodes(workRunContext.getNodeMapping(), workRunContext.getWorkId());
            List<WorkInstanceEntity> parentInstances = workInstanceRepository
                .findAllByWorkIdAndWorkflowInstanceId(parentNodes, workRunContext.getFlowInstanceId());
            boolean parentIsError = parentInstances.stream().anyMatch(e -> InstanceStatus.FAIL.equals(e.getStatus()));
            boolean parentIsBreak = parentInstances.stream().anyMatch(e -> InstanceStatus.BREAK.equals(e.getStatus()));
            boolean parentIsRunning = parentInstances.stream().anyMatch(
                e -> InstanceStatus.RUNNING.equals(e.getStatus()) || InstanceStatus.PENDING.equals(e.getStatus()));

            // 如果父级在运行中，直接中断
            if (parentIsRunning) {
                return;
            }

            if (parentIsError) {
                // 如果父级有错，则状态直接变更为失败
                workInstance.setStatus(InstanceStatus.FAIL);
                workInstance.setSubmitLog("父级执行失败");
                if (workInstance.getExecStartDateTime() != null) {
                    workInstance.setExecEndDateTime(new Date());
                    workInstance.setDuration(
                        (System.currentTimeMillis() - workInstance.getExecStartDateTime().getTime()) / 1000);
                }
            } else if (parentIsBreak || InstanceStatus.BREAK.equals(workInstance.getStatus())) {
                // 如果父级有中断，则状态直接变更为中断
                workInstance.setStatus(InstanceStatus.BREAK);
                workInstance.setExecEndDateTime(new Date());
                workInstance
                    .setDuration((System.currentTimeMillis() - workInstance.getExecStartDateTime().getTime()) / 1000);
            } else {
                // 作业状态改为运行中
                workInstance.setStatus(InstanceStatus.RUNNING);
            }

            // 保存作业状态并解锁
            workInstanceRepository.saveAndFlush(workInstance);
            locker.unlock(lockId);

            // 再次查询作业实例，如果状态为运行中，则可以开始运行作业
            workInstance = workInstanceRepository.findByWorkIdAndWorkflowInstanceId(workRunContext.getWorkId(),
                workRunContext.getFlowInstanceId());
            if (InstanceStatus.RUNNING.equals(workInstance.getStatus())) {

                // 作业开始执行，添加作业流实例日志，加锁防止同时写入
                Integer lockId = locker.lock("flow_log_" + workRunContext.getFlowInstanceId());
                WorkflowInstanceEntity workflowInstance =
                    workflowInstanceRepository.findById(workRunContext.getFlowInstanceId()).get();
                String runLog = workflowInstanceRepository.getWorkflowLog(workRunContext.getFlowInstanceId()) + "\n"
                    + LocalDateTime.now() + WorkLog.SUCCESS_INFO + "作业: 【" + workRunContext.getWorkName() + "】开始执行";
                workflowInstanceRepository.setWorkflowLog(workRunContext.getFlowInstanceId(), runLog);
                workflowInstance.setRunLog(runLog);
                workflowInstanceRepository.saveAndFlush(workflowInstance);
                locker.unlock(lockId);

                // 封装workRunContext
                if (Strings.isEmpty(workRunContext.getVersionId())) {
                    // 通过workId封装workRunContext
                    WorkEntity work = workRepository.findById(event.getWorkId()).get();
                    WorkConfigEntity workConfig = workConfigRepository.findById(work.getConfigId()).get();
                    workRunContext =
                        WorkUtils.genWorkRunContext(workInstance.getId(), workEvent.getId(), work, workConfig);
                } else {
                    // 通过versionId封装workRunContext
                    VipWorkVersionEntity workVersion = vipWorkVersionRepository.findById(event.getVersionId()).get();
                    workRunContext =
                        WorkUtils.genWorkRunContext(workInstance.getId(), workEvent.getId(), workVersion, event);
                }
            }
        }

        // 初始化作业事件
        WorkEventEntity workEvent = new WorkEventEntity();
        workEvent.setExecProcess(0);
        workEvent = workEventRepository.saveAndFlush(workEvent);
        WorkInstanceEntity workInstance = workInstanceRepository.findById(workRunContext.getInstanceId()).get();

        // 修改实例状态
        if (processNeverRun(workEvent, 1)) {
            workInstance.setSubmitLog(LocalDateTime.now() + WorkLog.SUCCESS_INFO + "开始提交作业 \n");
            workInstance.setStatus(InstanceStatus.RUNNING);
            workInstance.setExecStartDateTime(new Date());
            workInstance = workInstanceRepository.saveAndFlush(workInstance);
        }

        // 任务开始运行事件，异步发送消息
        if (processNeverRun(workEvent, 2)) {
            if (InstanceType.AUTO.equals(workInstance.getInstanceType())) {
                alarmService.sendWorkMessage(workInstance, AlarmEventType.START_RUN);
            }
        }

        try {

            // 开始执行作业，每次都要执行
            execute(workRunContext, workInstance);

            // 判断任务是否执行完
            if (processOver(workEvent.getId())) {

                // 更新作业实例成功状态
                workInstance = workInstanceRepository.findById(workRunContext.getInstanceId()).get();
                workInstance.setStatus(InstanceStatus.SUCCESS);
                workInstance.setExecEndDateTime(new Date());
                workInstance
                    .setDuration((System.currentTimeMillis() - workInstance.getExecStartDateTime().getTime()) / 1000);
                workInstance
                    .setSubmitLog(workInstance.getSubmitLog() + LocalDateTime.now() + WorkLog.SUCCESS_INFO + "执行成功 \n");
                workInstanceRepository.save(workInstance);

                // 保存工作流日志
                if (!Strings.isEmpty(workInstance.getWorkflowInstanceId())) {
                    WorkflowInstanceEntity workflowInstance =
                        workflowInstanceRepository.findById(workInstance.getWorkflowInstanceId()).get();
                    String runLog = workflowInstanceRepository.getWorkflowLog(workflowInstance.getId()) + "\n"
                        + LocalDateTime.now() + WorkLog.SUCCESS_INFO + "作业: 【" + workRunContext.getWorkName() + "】运行成功";
                    workflowInstance.setRunLog(runLog);
                    workflowInstanceRepository.setWorkflowLog(workflowInstance.getId(), runLog);
                }

                // 任务运行成功，异步发送消息
                if (InstanceType.AUTO.equals(workInstance.getInstanceType())) {
                    alarmService.sendWorkMessage(workInstance, AlarmEventType.RUN_SUCCESS);
                }
            }
        } catch (WorkRunException e) {

            log.error(e.getMsg(), e);

            // 重新获取当前最新实例
            workInstance = workInstanceRepository.findById(workRunContext.getInstanceId()).get();

            // 如果是已中止，直接不处理
            if (InstanceStatus.ABORT.equals(workInstance.getStatus())) {
                return;
            }

            // 更新作业实例失败状态
            workInstance.setStatus(InstanceStatus.FAIL);
            workInstance.setExecEndDateTime(new Date());
            workInstance
                .setDuration((System.currentTimeMillis() - workInstance.getExecStartDateTime().getTime()) / 1000);
            workInstance.setSubmitLog(
                workInstance.getSubmitLog() + e.getMsg() + LocalDateTime.now() + WorkLog.ERROR_INFO + "执行失败 \n");
            workInstanceRepository.save(workInstance);

            // 保存工作流日志
            if (!Strings.isEmpty(workInstance.getWorkflowInstanceId())) {
                WorkflowInstanceEntity workflowInstance =
                    workflowInstanceRepository.findById(workInstance.getWorkflowInstanceId()).get();
                String runLog = workflowInstanceRepository.getWorkflowLog(workflowInstance.getId()) + "\n"
                    + LocalDateTime.now() + WorkLog.SUCCESS_INFO + "作业: 【" + workRunContext.getWorkName() + "】运行失败";
                workflowInstance.setRunLog(runLog);
                workflowInstanceRepository.setWorkflowLog(workflowInstance.getId(), runLog);
            }

            // 任务运行失败，异步发送消息
            if (InstanceType.AUTO.equals(workInstance.getInstanceType())) {
                alarmService.sendWorkMessage(workInstance, AlarmEventType.RUN_FAIL);
            }

            // 异常执行结束
            workEvent.setExecProcess(999);
            workEventRepository.saveAndFlush(workEvent);
        }

        // 运行结束
        if (processOver(workEvent.getId())) {

            // 任务运行结束，异步发送消息
            if (InstanceType.AUTO.equals(workInstance.getInstanceType())) {
                alarmService.sendWorkMessage(workInstance, AlarmEventType.RUN_END);
            }

            // 删除事件
            workEventRepository.deleteById(workEvent.getId());

            // 删除定时器
            try {
                scheduler.unscheduleJob(TriggerKey.triggerKey("event_" + workInstance.getId()));
            } catch (SchedulerException e) {
                log.error(e.getMessage());
            }
        }

        // 执行完请求线程
        WORK_THREAD.remove(workInstance.getId());

        if ("如果是作业流") {
            // 判断工作流是否执行完毕，检查结束节点是否都运行完
            lockId = locker.lock(event.getFlowInstanceId());

            // 如果工作流被中止，则不需要执行下面的逻辑
            WorkflowInstanceEntity lastWorkflowInstance =
                workflowInstanceRepository.findById(event.getFlowInstanceId()).get();
            if (InstanceStatus.ABORTING.equals(lastWorkflowInstance.getStatus())) {
                return;
            }

            try {
                // 获取结束节点实例
                List<String> endNodes = WorkUtils.getEndNodes(event.getNodeMapping(), event.getNodeList());
                List<WorkInstanceEntity> endNodeInstance =
                    workInstanceRepository.findAllByWorkIdAndWorkflowInstanceId(endNodes, event.getFlowInstanceId());
                boolean flowIsOver = endNodeInstance.stream()
                    .allMatch(e -> InstanceStatus.FAIL.equals(e.getStatus())
                        || InstanceStatus.SUCCESS.equals(e.getStatus()) || InstanceStatus.ABORT.equals(e.getStatus())
                        || InstanceStatus.BREAK.equals(e.getStatus()));

                // 判断工作流是否执行完
                if (flowIsOver) {
                    boolean flowIsError =
                        endNodeInstance.stream().anyMatch(e -> InstanceStatus.FAIL.equals(e.getStatus()));
                    WorkflowInstanceEntity workflowInstance =
                        workflowInstanceRepository.findById(event.getFlowInstanceId()).get();
                    workflowInstance.setStatus(flowIsError ? InstanceStatus.FAIL : InstanceStatus.SUCCESS);
                    workflowInstance.setRunLog(workflowInstanceRepository.getWorkflowLog(event.getFlowInstanceId())
                        + "\n" + LocalDateTime.now() + (flowIsError ? WorkLog.ERROR_INFO : WorkLog.SUCCESS_INFO)
                        + (flowIsError ? "运行失败" : "运行成功"));
                    workflowInstance.setDuration(
                        (System.currentTimeMillis() - workflowInstance.getExecStartDateTime().getTime()) / 1000);
                    workflowInstance.setExecEndDateTime(new Date());

                    // 基线告警
                    if (flowIsError) {
                        // 执行失败，基线告警
                        if (InstanceType.AUTO.equals(workflowInstance.getInstanceType())) {
                            alarmService.sendWorkflowMessage(workflowInstance, AlarmEventType.RUN_FAIL);
                        }
                    } else {
                        // 执行成功，基线告警
                        if (InstanceType.AUTO.equals(workInstance.getInstanceType())) {
                            alarmService.sendWorkflowMessage(workflowInstance, AlarmEventType.RUN_SUCCESS);
                        }
                    }
                    // 执行结束，基线告警
                    if (InstanceType.AUTO.equals(workInstance.getInstanceType())) {
                        alarmService.sendWorkflowMessage(workflowInstance, AlarmEventType.RUN_END);
                    }

                    workflowInstanceRepository.saveAndFlush(workflowInstance);

                    // 清除缓存中的作业流日志
                    workflowInstanceRepository.deleteWorkflowLog(event.getFlowInstanceId());
                    return;
                }
            } finally {
                locker.unlock(lockId);
            }

            // 工作流没有执行完，解析推送子节点
            List<String> sonNodes = WorkUtils.getSonNodes(event.getNodeMapping(), event.getWorkId());
            List<WorkEntity> sonNodeWorks = workRepository.findAllByWorkIds(sonNodes);
            sonNodeWorks.forEach(work -> {
                WorkflowRunEvent metaEvent = new WorkflowRunEvent(work.getId(), work.getName(), event);
                WorkInstanceEntity sonWorkInstance =
                    workInstanceRepository.findByWorkIdAndWorkflowInstanceId(work.getId(), event.getFlowInstanceId());
                metaEvent.setVersionId(sonWorkInstance.getVersionId());
                eventPublisher.publishEvent(metaEvent);
            });
        }
    }

    public void syncAbort(WorkInstanceEntity workInstance) {

        this.abort(workInstance);
    }

}
