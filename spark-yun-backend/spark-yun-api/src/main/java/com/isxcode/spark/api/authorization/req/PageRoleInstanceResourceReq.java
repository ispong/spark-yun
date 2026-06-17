package com.isxcode.spark.api.authorization.req;

import com.isxcode.spark.backend.api.base.pojos.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PageRoleInstanceResourceReq extends BasePageRequest {

    @Schema(title = "资源类型", example = "CLUSTER")
    @NotEmpty(message = "资源类型不能为空")
    private String resourceType;
}
