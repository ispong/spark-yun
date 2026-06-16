package com.isxcode.spark.api.tenant.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ApplyTenantInviteReq {

    @NotEmpty(message = "邀请码不能为空")
    private String inviteCode;
}
