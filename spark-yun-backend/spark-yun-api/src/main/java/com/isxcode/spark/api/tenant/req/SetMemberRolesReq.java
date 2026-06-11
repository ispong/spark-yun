package com.isxcode.spark.api.tenant.req;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class SetMemberRolesReq {

    @NotEmpty(message = "用户id不能为空")
    private String userId;

    private List<String> roleIds;
}
