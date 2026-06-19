package com.isxcode.spark.api.user.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginRes {

    private String username;

    private String account;

    private String phone;

    private String email;

    private String remark;

    private Boolean hasPassword;

    private String token;

    private String refreshToken;

    private String tenantId;

    private String role;

    // 平台超级管理员
    private Boolean platformSuperAdmin;

    // 平台管理员
    private Boolean platformAdmin;

    // 平台成员
    private Boolean platformMember;

    // 租户超级管理员
    private Boolean tenantSuperAdmin;

    // 租户管理员
    private Boolean tenantAdmin;

    // 普通成员
    private Boolean tenantMember;

    private Boolean workspaceAllPermissions;

    private List<String> permissions;

    private List<String> frontendPermissionCodes;

    private String defaultArea;
}
