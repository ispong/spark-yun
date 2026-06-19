package com.isxcode.spark.config;

import com.isxcode.spark.modules.userlog.service.UserLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class UserLogCleanupQuartzJob implements Job {

    private final UserLogService userLogService;

    @Override
    public void execute(JobExecutionContext context) {

        userLogService.cleanExpiredLogs();
    }
}
