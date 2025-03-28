package com.isxcode.star.modules.work.run;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

/**
 * 作业定时器，触发作业运行.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkRunJob implements Job {

    private final WorkExecutorFactory workExecutorFactory;

    @Override
    public void execute(JobExecutionContext context) {

        WorkRunContext workRunContext = JSON.parseObject(
            String.valueOf(context.getJobDetail().getJobDataMap().get("WorkRunContext")), WorkRunContext.class);

        // 触发作业异步运行
        WorkExecutor workExecutor = workExecutorFactory.create(workRunContext.getWorkType());
        workExecutor.runWork(workRunContext);
    }
}
