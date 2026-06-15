package com.isxcode.spark.api.tenant.res;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChooseTenantRes {

    private String token;

    private String refreshToken;

    private String tenantId;

    private String role;

    private Boolean systemAdmin;

    private Boolean platformSuperAdmin;

    private Boolean platformAdmin;

    private Boolean tenantSuperAdmin;

    private Boolean tenantAdmin;

    private Boolean tenantMember;

    private Boolean normalAdmin;

    private Boolean workspaceAllPermissions;

    private List<String> permissions;

    private String defaultArea;
}
