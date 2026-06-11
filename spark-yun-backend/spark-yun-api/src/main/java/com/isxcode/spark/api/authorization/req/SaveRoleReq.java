package com.isxcode.spark.api.authorization.req;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class SaveRoleReq {

    private String id;

    @NotEmpty(message = "角色名称不能为空")
    private String name;

    @NotEmpty(message = "角色编码不能为空")
    private String code;

    private String status;

    private List<String> permissionCodes;
}
