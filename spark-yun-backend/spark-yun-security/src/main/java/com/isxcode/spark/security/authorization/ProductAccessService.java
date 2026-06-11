package com.isxcode.spark.security.authorization;

import com.isxcode.spark.api.tenant.constants.TenantStatus;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.api.user.constants.UserStatus;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
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

    private final MemberRoleRepository memberRoleRepository;

    private final OrgRepository orgRepository;

    private final OrgMemberRepository orgMemberRepository;

    private final OrgRoleRepository orgRoleRepository;

    public AccessSnapshot resolve(String userId, String tenantId) {

        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new IsxAppException("用户不存在"));
        validateUser(user);

        boolean systemAdmin = RoleType.SYS_ADMIN.equals(user.getRoleCode());
        boolean platformAdmin = systemAdmin || Boolean.TRUE.equals(user.getPlatformAdmin());
        if (systemAdmin) {
            return new AccessSnapshot(userId, null, true, true, false, false, Set.of());
        }
        if (Strings.isEmpty(tenantId) || "undefined".equals(tenantId)) {
            return new AccessSnapshot(userId, null, false, platformAdmin, false, false, Set.of());
        }

        TenantEntity tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new IsxAppException("当前租户不可用"));
        validateTenant(tenant);
        TenantUserEntity member = tenantUserRepository.findByTenantIdAndUserId(tenantId, userId)
            .orElseThrow(() -> new IsxAppException("用户不在租户中，请联系管理员!"));
        if (!UserStatus.ENABLE.equals(member.getStatus())) {
            throw new IsxAppException("用户被租户禁用，请联系管理员!");
        }

        boolean tenantAdmin = userId.equals(tenant.getAdminUserId());
        boolean normalAdmin = Boolean.TRUE.equals(member.getNormalAdmin());
        Set<String> permissions = tenantAdmin || normalAdmin ? Set.of() : resolveWorkspacePermissions(tenantId, userId);
        return new AccessSnapshot(userId, tenantId, false, platformAdmin, tenantAdmin, normalAdmin, permissions);
    }

    public Set<String> resolveWorkspacePermissions(String tenantId, String userId) {

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
            return Set.of();
        }
        Set<String> enabledRoleIds =
            roleRepository.findAllById(roleIds).stream().filter(role -> TenantStatus.ENABLE.equals(role.getStatus()))
                .map(RoleEntity::getId).collect(Collectors.toSet());
        if (enabledRoleIds.isEmpty()) {
            return Set.of();
        }
        return rolePermissionRepository.findAllByTenantIdAndRoleIdIn(tenantId, enabledRoleIds).stream()
            .map(RolePermissionEntity::getPermissionCode).collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasWorkspacePermission(AccessSnapshot access, String module, String action) {

        return access.hasAllWorkspacePermissions()
            || access.permissions().contains(WorkspacePermissionCatalog.code(module, action));
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
}
