package com.isxcode.spark.api.platform.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePlatformSettingReq {

    @Schema(title = "平台描述", example = "至轻云轻量级智能数据平台")
    @Size(max = 2000, message = "平台描述不能超过2000个字符")
    private String description;

    @Schema(title = "注册时是否创建租户", example = "true")
    private Boolean autoCreateTenant;

    @Schema(title = "浏览器标题文字", example = "至轻云")
    @Size(max = 100, message = "浏览器标题文字不能超过100个字符")
    private String browserTitle;

    @Schema(title = "主题色", example = "#f34c00")
    @Size(max = 20, message = "主题色不能超过20个字符")
    private String themeColor;

    @Schema(title = "浏览器标签图标")
    @Size(max = 5000000, message = "浏览器标签图标不能超过5MB")
    private String faviconUrl;

    @Schema(title = "顶部大Logo")
    @Size(max = 5000000, message = "顶部大Logo不能超过5MB")
    private String topLogoUrl;

    @Schema(title = "顶部小Logo")
    @Size(max = 5000000, message = "顶部小Logo不能超过5MB")
    private String topLogoSmallUrl;

    @Schema(title = "登录页主视觉图")
    @Size(max = 5000000, message = "登录页主视觉图不能超过5MB")
    private String loginMainImageUrl;

    @Schema(title = "是否开启行为日志", example = "false")
    private Boolean userLogEnabled;

    @Schema(title = "行为日志保留周期（天）", example = "180")
    @Min(value = 1, message = "行为日志保留周期不能小于1天")
    @Max(value = 3650, message = "行为日志保留周期不能超过3650天")
    private Integer userLogRetentionDays;
}
