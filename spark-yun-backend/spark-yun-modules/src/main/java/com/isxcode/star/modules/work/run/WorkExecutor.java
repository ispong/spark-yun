package com.isxcode.star.modules.work.run;

import static com.isxcode.star.common.config.CommonConfig.TENANT_ID;
import static com.isxcode.star.common.config.CommonConfig.USER_ID;

import com.isxcode.star.api.alarm.constants.AlarmEventType;
import com.isxcode.star.api.instance.constants.InstanceStatus;
import com.isxcode.star.api.instance.constants.InstanceType;
import com.isxcode.star.api.work.constants.WorkLog;
import com.isxcode.star.backend.api.base.exceptions.WorkRunException;
import com.isxcode.star.modules.alarm.service.AlarmService;
import com.isxcode.star.modules.work.entity.WorkEventEntity;
import com.isxcode.star.modules.work.entity.WorkInstanceEntity;
import com.isxcode.star.modules.work.repository.WorkEventRepository;
import com.isxcode.star.modules.work.repository.WorkInstanceRepository;
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
import org.springframework.scheduling.annotation.Async;


@Slf4j
@RequiredArgsConstructor
public abstract class WorkExecutor {

    public static final Map<String, Thread> WORK_THREAD = new HashMap<>();

    private final WorkInstanceRepository workInstanceRepository;

    private final WorkflowInstanceRepository workflowInstanceRepository;

    private final AlarmService alarmService;

    private final SqlFunctionService sqlFunctionService;

    private final WorkEventRepository workEventRepository;

    private final Scheduler scheduler;

    public abstract String getWorkType();

    protected abstract void execute(WorkRunContext workRunContext, WorkInstanceEntity workInstance);

    protected abstract void abort(WorkInstanceEntity workInstance);

    public WorkInstanceEntity updateInstance(WorkInstanceEntity workInstance, StringBuilder logBuilder) {

        workInstance.setSubmitLog(logBuilder.toString());
        return workInstanceRepository.saveAndFlush(workInstance);
    }

    @Async("sparkYunWorkThreadPool")
    public void asyncExecute(WorkRunContext workRunContext) {

        // 初始化异步线程中的上下文
        USER_ID.set(workRunContext.getUserId());
        TENANT_ID.set(workRunContext.getTenantId());

        syncExecute(workRunContext);
    }

    /**
     * 当前进程没有运行过
     */
    public boolean processNeverRun(WorkEventEntity workEvent, Integer nowIndex) {

        // 上一步状态保存
        if (workEvent.getExecProcess() + 1 == nowIndex) {
            workEvent.setExecProcess(nowIndex);
            workEventRepository.saveAndFlush(workEvent);
        }

        // 返回是否执行过
        return workEvent.getExecProcess() < nowIndex + 1;
    }

    /**
     * 当前进程没有运行过
     */
    public boolean processOver(String workEventId) {

        return workEventRepository.existsByIdAndExecProcess(workEventId, 999);
    }

    public void syncExecute(WorkRunContext workRunContext) {

        // 获取任务事件
        WorkEventEntity workEvent = workEventRepository.findById(workRunContext.getEventId()).get();
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
    }

    public void syncAbort(WorkInstanceEntity workInstance) {

        this.abort(workInstance);
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
}
