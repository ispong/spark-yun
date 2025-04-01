package com.isxcode.star.modules.work.run;

import com.alibaba.fastjson2.JSON;
import com.isxcode.star.backend.api.base.exceptions.IsxAppException;
import com.isxcode.star.modules.work.entity.WorkEventEntity;
import com.isxcode.star.modules.work.repository.WorkEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;

import static com.isxcode.star.common.config.CommonConfig.TENANT_ID;
import static com.isxcode.star.common.config.CommonConfig.USER_ID;

/**
 * 作业运行，触发定时器.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkRunJobFactory {

    private final Scheduler scheduler;

    private final WorkEventRepository workEventRepository;

    public void execute(WorkRunContext workRunContext) {

        // 设置用户id和租户id
        USER_ID.set(workRunContext.getUserId());
        TENANT_ID.set(workRunContext.getTenantId());

        // 初始化作业事件
        WorkEventEntity workEvent =
            WorkEventEntity.builder().eventProcess(0).eventContext(JSON.toJSONString(workRunContext)).build();
        workEvent = workEventRepository.saveAndFlush(workEvent);

        // 封装调度器上下文
        JobDataMap jobDataMap = new JobDataMap();
        workRunContext.setEventId(workEvent.getId());
        jobDataMap.put("workRunContext", JSON.toJSONString(workRunContext));

        // 初始化调度器，每1秒执行一次，每个作业都配置一个调度器刷新状态
        JobDetail jobDetail = JobBuilder.newJob(WorkRunJob.class).setJobData(jobDataMap).build();
        Trigger trigger = TriggerBuilder.newTrigger()
            .withSchedule(
                CronScheduleBuilder.cronSchedule("*/1 * * * * ? ").withMisfireHandlingInstructionFireAndProceed())
            .withIdentity("event_" + workRunContext.getEventId()).build();

        // 创建并触发调度器
        try {
            scheduler.scheduleJob(jobDetail, trigger);
            scheduler.getListenerManager().addJobListener(new QuartzJobErrorListener());
            scheduler.start();
        } catch (SchedulerException e) {
            // 一般不会报错
            log.error(e.getMessage(), e);
            throw new IsxAppException("刷新作业状态的调度器创建失败");
        }
    }
}
