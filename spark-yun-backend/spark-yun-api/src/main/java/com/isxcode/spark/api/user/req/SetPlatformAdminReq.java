package com.isxcode.spark.api.user.req;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetPlatformAdminReq {

    @NotEmpty(message = "用户id不能为空")
    private String userId;

    @NotNull(message = "平台管理员状态不能为空")
    private Boolean platformAdmin;
}
