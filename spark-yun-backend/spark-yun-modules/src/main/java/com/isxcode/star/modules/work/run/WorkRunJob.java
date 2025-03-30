package com.isxcode.star.modules.work.run;

import com.alibaba.fastjson2.JSON;
import com.isxcode.star.modules.work.repository.WorkEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;

import static com.isxcode.star.common.config.CommonConfig.TENANT_ID;
import static com.isxcode.star.common.config.CommonConfig.USER_ID;

/**
 * 作业调度器，触发作业运行.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkRunJob implements Job {

    private final WorkExecutorFactory workExecutorFactory;

    private final Scheduler scheduler;

    private final WorkEventRepository workEventRepository;

    @Override
    public void execute(JobExecutionContext context) {

        // 获取调度器上下文
        WorkRunContext workRunContext = JSON.parseObject(
            String.valueOf(context.getJobDetail().getJobDataMap().get("workRunContext")), WorkRunContext.class);

        // 异步配置租户id和用户id
        USER_ID.set(workRunContext.getUserId());
        TENANT_ID.set(workRunContext.getTenantId());

        // 如果事件查不到，直接执行删除
        if (!workEventRepository.existsById(workRunContext.getEventId())) {
            try {
                scheduler.unscheduleJob(TriggerKey.triggerKey("event_" + workRunContext.getEventId()));
            } catch (SchedulerException ex) {
                log.error(ex.getMessage(), ex);
            }
            return;
        }

        // 触发作业运行
        try {
            WorkExecutor workExecutor = workExecutorFactory.create(workRunContext.getWorkType());
            workExecutor.runWork(workRunContext);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            try {
                // 如果运行作业异常未捕获，先关闭调度器
                scheduler.unscheduleJob(TriggerKey.triggerKey("event_" + workRunContext.getEventId()));
            } catch (SchedulerException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }
}
