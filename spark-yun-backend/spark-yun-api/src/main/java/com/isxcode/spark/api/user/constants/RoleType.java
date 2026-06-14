package com.isxcode.spark.api.user.constants;

/** 角色. */
public interface RoleType {

    /** 平台超级管理员. */
    String platformSuperAdmin = "ROLE_SYS_ADMIN";

    /** 平台管理员. */
    String platformAdmin = "ROLE_PLATFORM_ADMIN";

    /** 租户超级管理员. */
    String tenantSuperAdmin = "ROLE_TENANT_ADMIN";

    /** 租户管理员. */
    String tenantAdmin = "ROLE_TENANT_NORMAL_ADMIN";

    /** 普通成员. */
    String tenantMember = "ROLE_TENANT_MEMBER";

    /** 匿名者权限. */
    String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";
}
