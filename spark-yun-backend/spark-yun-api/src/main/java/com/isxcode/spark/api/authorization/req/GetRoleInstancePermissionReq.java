package com.isxcode.spark.api.authorization.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class GetRoleInstancePermissionReq {

    @Schema(title = "角色id", example = "sy_f8402cd43898421687fcc7c8b98a359c")
    @NotEmpty(message = "角色id不能为空")
    private String roleId;

    @Schema(title = "资源类型", example = "CLUSTER")
    @NotEmpty(message = "资源类型不能为空")
    private String resourceType;
}
