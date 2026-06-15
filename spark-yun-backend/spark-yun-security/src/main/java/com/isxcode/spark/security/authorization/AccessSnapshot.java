package com.isxcode.spark.security.authorization;

import java.util.Set;

public record AccessSnapshot(String userId, String tenantId, boolean systemAdmin, boolean platformAdmin,
    boolean tenantAdmin, boolean normalAdmin, boolean workspaceAllPermissions, Set<String> permissions) {

    public boolean hasTenantAccess() {

        return tenantAdmin || normalAdmin || tenantId != null;
    }

    public boolean hasAllWorkspacePermissions() {

        return tenantAdmin || normalAdmin || workspaceAllPermissions;
    }
}
