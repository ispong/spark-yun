package com.isxcode.spark.api.user.constants;

/** 角色. */
public interface RoleType {

    /** 系统管理员权限. */
    String SYS_ADMIN = "ROLE_SYS_ADMIN";

    /** 平台管理员权限. */
    String PLATFORM_ADMIN = "ROLE_PLATFORM_ADMIN";

    /** 租户管理员权限. */
    String TENANT_ADMIN = "ROLE_TENANT_ADMIN";

    /** 租户普通管理员权限. */
    String TENANT_NORMAL_ADMIN = "ROLE_TENANT_NORMAL_ADMIN";

    /** 租户成员权限. */
    String TENANT_MEMBER = "ROLE_TENANT_MEMBER";

    /** 普通成员. */
    String NORMAL_MEMBER = "ROLE_NORMAL_MEMBER";

    /** 匿名者权限. */
    String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";
}
