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

    private List<String> frontendPermissionCodes;

    private List<String> backendPermissionCodes;

    private List<PermissionModuleRes> menuPermissions;

    private List<PermissionModuleRes> buttonPermissions;

    private List<PermissionModuleRes> interfacePermissions;

    private List<PermissionModuleRes> dataPermissions;

    @Data
    @Builder
    public static class PermissionModuleRes {

        private String code;

        private String name;

        private List<PermissionItemRes> permissions;
    }

    @Data
    @Builder
    public static class PermissionItemRes {

        private String code;

        private String name;

        private String permissionCode;

        private String method;

        private String path;

        private String action;
    }
}
