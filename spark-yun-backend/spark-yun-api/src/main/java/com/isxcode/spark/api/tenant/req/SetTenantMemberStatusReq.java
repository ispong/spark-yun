package com.isxcode.spark.api.tenant.req;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SetTenantMemberStatusReq {

    @NotEmpty(message = "租户成员id不能为空")
    private String tenantUserId;

    @Pattern(regexp = "ENABLE|DISABLE", message = "成员状态不正确")
    private String status;
}
