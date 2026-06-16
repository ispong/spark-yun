package com.isxcode.spark.api.platform.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePlatformSettingReq {

    @Schema(title = "平台描述", example = "至轻云轻量级智能数据平台")
    @Size(max = 2000, message = "平台描述不能超过2000个字符")
    private String description;

    @Schema(title = "注册时是否创建租户", example = "true")
    private Boolean autoCreateTenant;
}
