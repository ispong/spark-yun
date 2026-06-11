package com.isxcode.spark.api.authorization.req;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class SaveOrgReq {

    private String id;

    private String parentId;

    @NotEmpty(message = "组织名称不能为空")
    private String name;

    private List<String> userIds;

    private List<String> roleIds;
}
