package com.isxcode.spark.api.authorization.res;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleInstancePermissionRes {

    private String roleId;

    private String resourceType;

    private Boolean allEnabled;

    private List<String> resourceIds;
}
