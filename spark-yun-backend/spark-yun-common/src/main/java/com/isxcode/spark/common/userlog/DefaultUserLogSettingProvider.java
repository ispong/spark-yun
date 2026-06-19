package com.isxcode.spark.common.userlog;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(UserLogSettingProvider.class)
public class DefaultUserLogSettingProvider implements UserLogSettingProvider {

    @Override
    public boolean enabled() {

        return false;
    }

    @Override
    public int retentionDays() {

        return 180;
    }
}
