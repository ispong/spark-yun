package com.isxcode.spark.api.authorization.res;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleRes {

    private String id;

    private String name;

    private String code;

    private String remark;

    private String status;

    private List<String> permissionCodes;
}
