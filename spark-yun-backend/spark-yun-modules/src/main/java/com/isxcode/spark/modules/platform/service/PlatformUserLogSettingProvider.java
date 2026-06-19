package com.isxcode.spark.modules.platform.service;

import com.isxcode.spark.common.userlog.UserLogSettingProvider;
import com.isxcode.spark.modules.platform.repository.PlatformSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformUserLogSettingProvider implements UserLogSettingProvider {

    private final PlatformSettingRepository platformSettingRepository;

    @Override
    public boolean enabled() {

        return platformSettingRepository.findBySettingKey(PlatformSettingService.GLOBAL_SETTING_KEY)
            .map(setting -> Boolean.TRUE.equals(setting.getUserLogEnabled())).orElse(false);
    }

    @Override
    public int retentionDays() {

        return platformSettingRepository.findBySettingKey(PlatformSettingService.GLOBAL_SETTING_KEY)
            .map(setting -> setting.getUserLogRetentionDays() == null
                ? PlatformSettingService.DEFAULT_USER_LOG_RETENTION_DAYS
                : setting.getUserLogRetentionDays())
            .orElse(PlatformSettingService.DEFAULT_USER_LOG_RETENTION_DAYS);
    }
}
