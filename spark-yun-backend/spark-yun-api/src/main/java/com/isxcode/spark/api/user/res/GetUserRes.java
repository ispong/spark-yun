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
public class GetUserRes {

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

    private Boolean systemAdmin;

    private Boolean platformSuperAdmin;

    private Boolean platformAdmin;

    private Boolean platformMember;

    private Boolean tenantSuperAdmin;

    private Boolean tenantAdmin;

    private Boolean tenantMember;

    private Boolean normalAdmin;

    private Boolean workspaceAllPermissions;

    private List<String> permissions;

    private String defaultArea;
}
