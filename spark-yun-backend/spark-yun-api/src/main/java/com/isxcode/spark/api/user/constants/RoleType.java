package com.isxcode.spark.api.user.constants;

/**
 * 角色.
 */
public interface RoleType {

    /**
     * 平台超级管理员.
     */
    String PLATFORM_SUPER_ADMIN = "PLATFORM_SUPER_ADMIN";

    /**
     * 平台管理员.
     */
    String PLATFORM_ADMIN = "PLATFORM_ADMIN";

    /**
     * 平台成员.
     */
    String PLATFORM_MEMBER = "PLATFORM_MEMBER";

    /**
     * 租户超级管理员.
     */
    String TENANT_SUPER_ADMIN = "TENANT_SUPER_ADMIN";

    /**
     * 租户管理员.
     */
    String TENANT_ADMIN = "TENANT_ADMIN";

    /**
     * 普通成员.
     */
    String TENANT_MEMBER = "TENANT_MEMBER";

    /**
     * 匿名者权限.
     */
    String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";
}
