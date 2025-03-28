package com.isxcode.star.modules.work.event;

import com.alibaba.fastjson2.JSON;
import com.isxcode.star.backend.api.base.exceptions.IsxAppException;
import com.isxcode.star.modules.work.run.WorkRunContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class WorkRunFactory {

    private final Scheduler scheduler;

    public void execute(WorkRunContext workRunContext) {

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("WorkRunContext", JSON.toJSONString(workRunContext));

        // 提交作业
        JobDetail jobDetail = JobBuilder.newJob(WorkRunJob.class).setJobData(jobDataMap).build();
        Trigger trigger = TriggerBuilder.newTrigger().withSchedule(CronScheduleBuilder.cronSchedule("0/3 * * * * ? "))
            .withIdentity("event_" + workRunContext.getInstanceId()).build();

        try {
            scheduler.scheduleJob(jobDetail, trigger);
            scheduler.start();
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException("运行作业失败");
        }
    }
}
