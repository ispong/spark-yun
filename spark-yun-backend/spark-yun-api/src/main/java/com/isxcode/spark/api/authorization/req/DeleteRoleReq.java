package com.isxcode.spark.api.authorization.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class DeleteRoleReq {

    @NotEmpty(message = "角色id不能为空")
    private String roleId;
}
