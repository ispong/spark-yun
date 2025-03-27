package com.isxcode.star.modules.work.service;

import com.isxcode.star.common.locker.Locker;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WorkEventService {

    private final Locker locker;

    @Scheduled(cron = "0/3 * * * * ? ")
    public void schedule() throws InterruptedException {

        // 开多个线程，刷新状态，防止速度太慢了

        // 对象，直接使用fastjson封装请求体

        // 获取所有运行中的作业事件

        // 封装Event事件体，再推一次作业

        // 获取手动所有的运行中的作业流里的作业

        // 封装Flow事件体，再推一次事件

        // 获取定时调度所有的运行中的作业流里的作业

        // 封装Flow事件体，再推一次事件
    }
}
