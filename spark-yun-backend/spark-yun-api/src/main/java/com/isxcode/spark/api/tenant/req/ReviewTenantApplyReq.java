package com.isxcode.spark.api.tenant.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ReviewTenantApplyReq {

    @NotEmpty(message = "租户成员id不能为空")
    private String tenantUserId;
}
