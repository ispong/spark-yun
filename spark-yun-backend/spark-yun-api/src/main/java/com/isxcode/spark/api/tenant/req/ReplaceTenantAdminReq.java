package com.isxcode.spark.api.tenant.req;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ReplaceTenantAdminReq {

    @NotEmpty(message = "租户id不能为空")
    private String tenantId;

    @NotEmpty(message = "新租户管理员不能为空")
    private String newAdminUserId;

    @Pattern(regexp = "KEEP|REMOVE", message = "旧租户管理员处理方式不正确")
    private String oldAdminAction = "KEEP";
}
