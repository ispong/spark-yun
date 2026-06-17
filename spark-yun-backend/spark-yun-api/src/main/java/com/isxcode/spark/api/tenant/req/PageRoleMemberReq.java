package com.isxcode.spark.api.tenant.req;

import com.isxcode.spark.backend.api.base.pojos.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PageRoleMemberReq extends BasePageRequest {

    @Schema(title = "租户id", example = "sy_f8402cd43898421687fcc7c8b98a359c")
    private String tenantId;

    @Schema(title = "角色id", example = "sy_f8402cd43898421687fcc7c8b98a359c")
    @NotEmpty(message = "角色id不能为空")
    private String roleId;
}
