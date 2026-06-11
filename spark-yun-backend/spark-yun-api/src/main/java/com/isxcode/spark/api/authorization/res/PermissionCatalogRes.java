package com.isxcode.spark.api.authorization.res;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionCatalogRes {

    private List<String> modules;

    private List<String> actions;

    private List<String> permissionCodes;
}
