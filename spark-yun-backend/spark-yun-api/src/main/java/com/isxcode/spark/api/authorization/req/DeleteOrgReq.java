package com.isxcode.spark.api.authorization.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class DeleteOrgReq {

    @NotEmpty(message = "组织id不能为空")
    private String orgId;
}
