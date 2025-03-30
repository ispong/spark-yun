package com.isxcode.star.modules.work.run;

import com.alibaba.fastjson2.JSON;
import com.isxcode.star.backend.api.base.exceptions.IsxAppException;
import com.isxcode.star.modules.work.entity.WorkEventEntity;
import com.isxcode.star.modules.work.repository.WorkEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;

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

        // 初始化作业事件
        WorkEventEntity workEvent = new WorkEventEntity();
        workEvent.setEventProcess(0);
        workEvent.setEventContext(JSON.toJSONString(workRunContext));
        workEvent = workEventRepository.saveAndFlush(workEvent);

        // 封装调度器上下文
        JobDataMap jobDataMap = new JobDataMap();
        workRunContext.setEventId(workEvent.getId());
        jobDataMap.put("workRunContext", JSON.toJSONString(workRunContext));

        // 初始化调度器，每3秒执行一次
        JobDetail jobDetail = JobBuilder.newJob(WorkRunJob.class).setJobData(jobDataMap).build();
        Trigger trigger = TriggerBuilder.newTrigger()
            .withSchedule(
                CronScheduleBuilder.cronSchedule("*/3 * * * * ? ").withMisfireHandlingInstructionFireAndProceed())
            .withIdentity("event_" + workRunContext.getEventId()).build();

        // 创建并触发调度器
        try {
            scheduler.scheduleJob(jobDetail, trigger);
            scheduler.getListenerManager().addJobListener(new QuartzJobErrorListener());
            scheduler.start();
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException("运行作业失败");
        }
    }
}
