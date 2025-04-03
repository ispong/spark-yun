package com.isxcode.star.modules.work.run;

import com.alibaba.fastjson2.JSON;
import com.isxcode.star.api.instance.constants.InstanceStatus;
import com.isxcode.star.common.locker.Locker;
import com.isxcode.star.modules.work.repository.WorkEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;


import static com.isxcode.star.common.config.CommonConfig.TENANT_ID;
import static com.isxcode.star.common.config.CommonConfig.USER_ID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkRunJob implements Job {

    private final WorkExecutorFactory workExecutorFactory;

    private final Scheduler scheduler;

    private final WorkEventRepository workEventRepository;

    private final Locker locker;

    @Override
    public void execute(JobExecutionContext context) {

        // 获取调度器上下文
        WorkRunContext workRunContext = JSON.parseObject(
            String.valueOf(context.getJobDetail().getJobDataMap().get("workRunContext")), WorkRunContext.class);

        // 判断上一个锁是否被占用，让调度器一个一个执行，上一个没有执行完，下一个不让执行
        if (locker.isLocked("scheduler_" + workRunContext.getEventId())) {
            return;
        }

        // 当前执行器加锁
        Integer lockId = locker.lockOnly("scheduler_" + workRunContext.getEventId());

        // 异步配置租户id和用户id
        USER_ID.set(workRunContext.getUserId());
        TENANT_ID.set(workRunContext.getTenantId());

        // 触发作业运行
        String runStatus;
        try {
            WorkExecutor workExecutor = workExecutorFactory.create(workRunContext.getWorkType());
            runStatus = workExecutor.runWork(workRunContext);
        } catch (Exception e) {
            // 作业运行有异常，则作业运行结束
            log.error(e.getMessage(), e);
            runStatus = InstanceStatus.FINISHED;
        }

        // 作业运行结束，调度器和事件都要删除
        if (InstanceStatus.FINISHED.equals(runStatus)) {

            try {
                workEventRepository.deleteByIdAndFlush(workRunContext.getEventId());
            } catch (Exception ignore) {
                // 直接删除，异常不处理
            }

            try {
                scheduler.unscheduleJob(TriggerKey.triggerKey("event_" + workRunContext.getEventId()));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        // 解锁，让下一个调度器可以执行
        locker.unlock(lockId);
    }
}
