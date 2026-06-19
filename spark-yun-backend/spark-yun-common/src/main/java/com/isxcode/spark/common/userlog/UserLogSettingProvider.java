package com.isxcode.spark.common.userlog;

public interface UserLogSettingProvider {

    boolean enabled();

    int retentionDays();
}
