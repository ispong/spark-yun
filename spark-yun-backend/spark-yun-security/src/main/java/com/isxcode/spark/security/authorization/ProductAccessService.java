package com.isxcode.spark.security.authorization;

import com.isxcode.spark.api.authorization.constants.RoleInstanceResourceType;
import com.isxcode.spark.api.tenant.constants.TenantStatus;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.api.user.constants.UserStatus;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.common.jpa.DataScopeContext;
import com.isxcode.spark.common.jpa.JpaTenantContext;
import com.isxcode.spark.security.user.TenantEntity;
import com.isxcode.spark.security.user.TenantRepository;
import com.isxcode.spark.security.user.TenantUserEntity;
import com.isxcode.spark.security.user.TenantUserRepository;
import com.isxcode.spark.security.user.UserEntity;
import com.isxcode.spark.security.user.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class, readOnly = true)
public class ProductAccessService {

    private final UserRepository userRepository;

    private final TenantRepository tenantRepository;

    private final TenantUserRepository tenantUserRepository;

    private final RoleRepository roleRepository;

    private final RolePermissionRepository rolePermissionRepository;

    private final RoleInstancePermissionRepository roleInstancePermissionRepository;

    private final MemberRoleRepository memberRoleRepository;

    private final OrgRepository orgRepository;

    private final OrgMemberRepository orgMemberRepository;

    private final OrgRoleRepository orgRoleRepository;

    public AccessSnapshot resolve(String userId, String tenantId) {

        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new IsxAppException("用户不存在"));
        validateUser(user);

        boolean platformSuperAdmin = RoleType.PLATFORM_SUPER_ADMIN.equals(user.getRoleCode());
        boolean platformAdmin =
            RoleType.PLATFORM_ADMIN.equals(user.getRoleCode()) || Boolean.TRUE.equals(user.getPlatformAdmin());
        if (platformSuperAdmin) {
            return new AccessSnapshot(userId, null, true, true, false, false, false, false, Set.of(), Set.of(),
                Set.of(), allResourceScope(), allResourceScope(), allResourceScope());
        }
        if (Strings.isEmpty(tenantId) || "undefined".equals(tenantId)) {
            return new AccessSnapshot(userId, null, false, platformAdmin, false, false, false, false, Set.of(),
                Set.of(), Set.of(), allResourceScope(), allResourceScope(), allResourceScope());
        }

        TenantEntity tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new IsxAppException("当前租户不可用"));
        validateTenant(tenant);
        TenantUserEntity member = tenantUserRepository.findByTenantIdAndUserId(tenantId, userId)
            .orElseThrow(() -> new IsxAppException("用户不在租户中，请联系管理员!"));
        if (!UserStatus.ENABLE.equals(member.getStatus())) {
            throw new IsxAppException("用户被租户禁用，请联系管理员!");
        }

        boolean tenantAdmin =
            userId.equals(tenant.getAdminUserId()) || RoleType.TENANT_SUPER_ADMIN.equals(member.getRoleCode());
        boolean normalAdmin =
            Boolean.TRUE.equals(member.getNormalAdmin()) || RoleType.TENANT_ADMIN.equals(member.getRoleCode());
        WorkspacePermissionResult workspacePermission =
            tenantAdmin || normalAdmin ? WorkspacePermissionResult.allPermissions()
                : resolveWorkspacePermissions(tenantId, userId);
        Set<String> permissions = new HashSet<>();
        permissions.addAll(workspacePermission.frontendPermissions());
        permissions.addAll(workspacePermission.backendPermissions());
        return new AccessSnapshot(userId, tenantId, false, platformAdmin, tenantAdmin, normalAdmin,
            workspacePermission.menuAllPermissions(), workspacePermission.apiAllPermissions(), Set.copyOf(permissions),
            workspacePermission.frontendPermissions(), workspacePermission.backendPermissions(),
            workspacePermission.clusterScope(), workspacePermission.datasourceScope(), workspacePermission.fileScope());
    }

    public WorkspacePermissionResult resolveWorkspacePermissions(String tenantId, String userId) {

        return JpaTenantContext.allData(() -> doResolveWorkspacePermissions(tenantId, userId));
    }

    private WorkspacePermissionResult doResolveWorkspacePermissions(String tenantId, String userId) {

        Set<String> roleIds = memberRoleRepository.findAllByTenantIdAndUserId(tenantId, userId).stream()
            .map(MemberRoleEntity::getRoleId).collect(Collectors.toCollection(HashSet::new));

        Set<String> orgIds = orgMemberRepository.findAllByTenantIdAndUserId(tenantId, userId).stream()
            .map(OrgMemberEntity::getOrgId).collect(Collectors.toCollection(HashSet::new));
        if (!orgIds.isEmpty()) {
            Map<String, OrgEntity> orgMap = new HashMap<>();
            orgRepository.findAllByTenantIdOrderByCreateDateTimeAsc(tenantId)
                .forEach(org -> orgMap.put(org.getId(), org));
            Set<String> inheritedOrgIds = new HashSet<>(orgIds);
            orgIds.forEach(orgId -> collectParentOrgIds(orgId, orgMap, inheritedOrgIds));
            roleIds.addAll(orgRoleRepository.findAllByTenantIdAndOrgIdIn(tenantId, inheritedOrgIds).stream()
                .map(OrgRoleEntity::getRoleId).collect(Collectors.toSet()));
        }

        if (roleIds.isEmpty()) {
            return WorkspacePermissionResult.allPermissions();
        }
        Set<String> enabledRoleIds = roleRepository.findAllByTenantIdAndIdIn(tenantId, roleIds).stream()
            .filter(role -> TenantStatus.ENABLE.equals(role.getStatus())).map(RoleEntity::getId)
            .collect(Collectors.toSet());
        if (enabledRoleIds.isEmpty()) {
            return new WorkspacePermissionResult(false, false, Set.of(), Set.of(), allResourceScope(),
                allResourceScope(), allResourceScope());
        }
        Set<String> frontendPermissions = enabledRoleIds.stream()
            .flatMap(roleId -> rolePermissionRepository.findAllByTenantIdAndRoleId(tenantId, roleId).stream())
            .filter(
                permission -> WorkspacePermissionCatalog.FRONTEND_PERMISSION_TYPE.equals(permission.getPermissionType())
                    || (permission.getPermissionType() == null
                        && WorkspacePermissionCatalog.isMenuPermissionCode(permission.getPermissionCode())))
            .map(RolePermissionEntity::getPermissionCode).collect(Collectors.toUnmodifiableSet());
        Set<String> backendPermissions = enabledRoleIds.stream()
            .flatMap(roleId -> rolePermissionRepository.findAllByTenantIdAndRoleId(tenantId, roleId).stream())
            .filter(
                permission -> WorkspacePermissionCatalog.BACKEND_PERMISSION_TYPE.equals(permission.getPermissionType())
                    || (permission.getPermissionType() == null
                        && WorkspacePermissionCatalog.isBackendPermissionCode(permission.getPermissionCode())))
            .map(RolePermissionEntity::getPermissionCode).collect(Collectors.toUnmodifiableSet());
        Map<String, RoleInstancePermissionEntity> instancePermissions =
            roleInstancePermissionRepository.findAllByTenantIdAndRoleIdIn(tenantId, enabledRoleIds).stream()
                .collect(Collectors.toMap(permission -> permission.getRoleId() + ":" + permission.getResourceType(),
                    permission -> permission, (left, right) -> right));
        return new WorkspacePermissionResult(frontendPermissions.contains(WorkspacePermissionCatalog.MENU_ALL),
            backendPermissions.contains(WorkspacePermissionCatalog.API_ALL), frontendPermissions, backendPermissions,
            resolveResourceScope(enabledRoleIds, instancePermissions, RoleInstanceResourceType.CLUSTER),
            resolveResourceScope(enabledRoleIds, instancePermissions, RoleInstanceResourceType.DATASOURCE),
            resolveResourceScope(enabledRoleIds, instancePermissions, RoleInstanceResourceType.RESOURCE_FILE));
    }

    private DataScopeContext.ResourceScope resolveResourceScope(Set<String> roleIds,
        Map<String, RoleInstancePermissionEntity> permissions, String resourceType) {

        Set<String> resourceIds = new HashSet<>();
        for (String roleId : roleIds) {
            RoleInstancePermissionEntity permission = permissions.get(roleId + ":" + resourceType);
            if (permission == null) {
                return allResourceScope();
            }
            Set<String> permissionResourceIds = splitResourceIds(permission.getResourceIds());
            if (permissionResourceIds.contains(RoleInstanceResourceType.ALL)) {
                return allResourceScope();
            }
            resourceIds.addAll(permissionResourceIds);
        }
        return new DataScopeContext.ResourceScope(false, Set.copyOf(resourceIds));
    }

    private Set<String> splitResourceIds(String resourceIds) {

        if (Strings.isEmpty(resourceIds)) {
            return Set.of();
        }
        return java.util.Arrays.stream(resourceIds.split(",")).filter(id -> !Strings.isEmpty(id))
            .collect(Collectors.toSet());
    }

    private DataScopeContext.ResourceScope allResourceScope() {

        return new DataScopeContext.ResourceScope(true, Set.of());
    }

    public boolean hasWorkspacePermission(AccessSnapshot access, String module, String action) {

        return access.hasAllWorkspacePermissions()
            || access.frontendPermissionCodes().contains(WorkspacePermissionCatalog.menuCode(module));
    }

    public boolean hasWorkspaceApiPermission(AccessSnapshot access, String module, String method, String path) {

        return access.hasAllApiPermissions()
            || access.backendPermissionCodes()
                .contains(WorkspacePermissionCatalog.code(module, WorkspacePermissionCatalog.resolveAction(path)))
            || access.backendPermissionCodes().contains(WorkspacePermissionCatalog.apiCode(module, method, path));
    }

    public boolean hasWorkspaceDataPermission(AccessSnapshot access, String module, String action) {

        String dataAction = switch (action) {
            case "view" -> "read";
            case "create" -> "create";
            case "edit" -> "update";
            case "delete" -> "delete";
            default -> null;
        };
        return dataAction == null || access.hasAllApiPermissions()
            || !WorkspacePermissionCatalog.hasDataPermissions(access.backendPermissionCodes())
            || access.backendPermissionCodes().contains(WorkspacePermissionCatalog.dataCode(module, dataAction));
    }

    private void collectParentOrgIds(String orgId, Map<String, OrgEntity> orgMap, Set<String> result) {

        String currentId = orgId;
        Set<String> visited = new HashSet<>();
        while (currentId != null && visited.add(currentId)) {
            OrgEntity current = orgMap.get(currentId);
            if (current == null || Strings.isEmpty(current.getParentId())) {
                return;
            }
            currentId = current.getParentId();
            result.add(currentId);
        }
    }

    private void validateUser(UserEntity user) {

        if (!UserStatus.ENABLE.equals(user.getStatus())) {
            throw new IsxAppException("用户被禁用，请联系管理员!");
        }
        if (user.getValidStartDateTime() != null && user.getValidEndDateTime() != null
            && (LocalDateTime.now().isBefore(user.getValidStartDateTime())
                || LocalDateTime.now().isAfter(user.getValidEndDateTime()))) {
            throw new IsxAppException("用户账号不在有效期内");
        }
    }

    private void validateTenant(TenantEntity tenant) {

        if (!TenantStatus.ENABLE.equals(tenant.getStatus())) {
            throw new IsxAppException("当前租户已被禁用");
        }
        if (tenant.getValidStartDateTime() != null && tenant.getValidEndDateTime() != null
            && (LocalDateTime.now().isBefore(tenant.getValidStartDateTime())
                || LocalDateTime.now().isAfter(tenant.getValidEndDateTime()))) {
            throw new IsxAppException("当前租户不在有效期内");
        }
    }

    public record WorkspacePermissionResult(boolean menuAllPermissions, boolean apiAllPermissions,
        Set<String> frontendPermissions, Set<String> backendPermissions, DataScopeContext.ResourceScope clusterScope,
        DataScopeContext.ResourceScope datasourceScope, DataScopeContext.ResourceScope fileScope) {

        public static WorkspacePermissionResult allPermissions() {

            DataScopeContext.ResourceScope allResourceScope = new DataScopeContext.ResourceScope(true, Set.of());
            return new WorkspacePermissionResult(true, true, Set.of(), Set.of(), allResourceScope, allResourceScope,
                allResourceScope);
        }
    }
}
