package com.isxcode.star.modules.work.event;

import com.alibaba.fastjson2.JSON;
import com.isxcode.star.modules.work.run.WorkExecutor;
import com.isxcode.star.modules.work.run.WorkExecutorFactory;
import com.isxcode.star.modules.work.run.WorkRunContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;


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
        workExecutor.asyncExecute(workRunContext);
    }
}
