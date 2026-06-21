package com.isxcode.spark.modules.work.run;

import com.alibaba.fastjson2.JSON;
import com.isxcode.spark.api.work.constants.QuartzPrefix;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.modules.work.entity.WorkEventEntity;
import com.isxcode.spark.modules.work.repository.WorkEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.isxcode.spark.common.security.ContextHolder;


@Slf4j
@Component
@RequiredArgsConstructor
public class WorkRunJobFactory {

    private final Scheduler scheduler;

    private final WorkEventRepository workEventRepository;

    public void run(WorkRunContext workRunContext) {

        ContextHolder.setUserId(workRunContext.getUserId());
        ContextHolder.setTenantId(workRunContext.getTenantId());

        // 初始化作业运行事件
        WorkEventEntity workEvent =
            WorkEventEntity.builder().eventProcess(0).eventContext(JSON.toJSONString(workRunContext)).build();
        workEvent = workEventRepository.save(workEvent);
        String workEventId = workEvent.getId();

        Runnable scheduleWork = () -> {
            // 封装调度器的运行参数
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(QuartzPrefix.USER_ID, workRunContext.getUserId());
            jobDataMap.put(QuartzPrefix.TENANT_ID, workRunContext.getTenantId());
            jobDataMap.put(QuartzPrefix.WORK_TYPE, workRunContext.getWorkType());
            jobDataMap.put(QuartzPrefix.WORK_EVENT_TYPE, workRunContext.getEventType());
            jobDataMap.put(QuartzPrefix.WORK_EVENT_ID, workEventId);

            // 初始化调度器，立即执行，并且每1秒执行一次.
            JobDetail jobDetail = JobBuilder.newJob(WorkRunJob.class).setJobData(jobDataMap).build();
            Trigger trigger = TriggerBuilder.newTrigger().startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1).repeatForever()
                    .withMisfireHandlingInstructionNextWithExistingCount())
                .withIdentity(QuartzPrefix.WORK_RUN_PROCESS + workEventId).build();

            // 创建并触发调度器
            try {
                scheduler.scheduleJob(jobDetail, trigger);
                if (scheduler.getListenerManager().getJobListener("workRunJobErrorListener") == null) {
                    scheduler.getListenerManager().addJobListener(new QuartzJobErrorListener());
                }
                if (!scheduler.isStarted()) {
                    scheduler.start();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new IsxAppException("作业运行异常: " + e.getMessage());
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {

                    scheduleWork.run();
                }
            });
        } else {
            scheduleWork.run();
        }
    }
}
