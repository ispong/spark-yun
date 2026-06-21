package com.isxcode.spark.api.platform.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetPlatformSettingRes {

    private String description;

    private Boolean autoCreateTenant;

    private Integer defaultTenantMemberNum;

    private Integer defaultTenantWorkflowNum;

    private Integer defaultTenantValidDays;

    private String browserTitle;

    private String themeColor;

    private String faviconUrl;

    private String topLogoUrl;

    private String topLogoSmallUrl;

    private String loginMainImageUrl;

    private Boolean userLogEnabled;

    private Integer userLogRetentionDays;
}
