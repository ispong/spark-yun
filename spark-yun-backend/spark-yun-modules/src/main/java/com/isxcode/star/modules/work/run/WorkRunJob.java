package com.isxcode.star.modules.work.run;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;

import static com.isxcode.star.common.config.CommonConfig.TENANT_ID;
import static com.isxcode.star.common.config.CommonConfig.USER_ID;

/**
 * 作业定时器，触发作业运行.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkRunJob implements Job {

    private final WorkExecutorFactory workExecutorFactory;

    private final Scheduler scheduler;

    @Override
    public void execute(JobExecutionContext context) {

        // 获取定时器上下文
        WorkRunContext workRunContext = JSON.parseObject(
            String.valueOf(context.getJobDetail().getJobDataMap().get("workRunContext")), WorkRunContext.class);

        // 初始化租户id和用户id
        USER_ID.set(workRunContext.getUserId());
        TENANT_ID.set(workRunContext.getTenantId());

        // 触发作业异步运行
        WorkExecutor workExecutor = workExecutorFactory.create(workRunContext.getWorkType());
        try {
            workExecutor.runWork(workRunContext);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            try {
                scheduler.unscheduleJob(TriggerKey.triggerKey("event_" + workRunContext.getInstanceId()));
            } catch (SchedulerException ex) {
                log.error(ex.getMessage(), ex);
            }
        }

    }
}
