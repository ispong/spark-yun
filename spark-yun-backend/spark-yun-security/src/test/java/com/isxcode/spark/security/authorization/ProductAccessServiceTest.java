package com.isxcode.spark.security.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductAccessServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantUserRepository tenantUserRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private MemberRoleRepository memberRoleRepository;

    @Mock
    private OrgRepository orgRepository;

    @Mock
    private OrgMemberRepository orgMemberRepository;

    @Mock
    private OrgRoleRepository orgRoleRepository;

    @InjectMocks
    private ProductAccessService productAccessService;

    @Test
    void resolvesDirectOrganizationAndParentOrganizationPermissions() {

        UserEntity user = enabledUser();
        TenantEntity tenant = enabledTenant();
        TenantUserEntity member = enabledMember(false);
        when(userRepository.findById("user")).thenReturn(Optional.of(user));
        when(tenantRepository.findById("tenant")).thenReturn(Optional.of(tenant));
        when(tenantUserRepository.findByTenantIdAndUserId("tenant", "user")).thenReturn(Optional.of(member));

        MemberRoleEntity directRole = memberRole("direct");
        OrgMemberEntity orgMember = orgMember("child");
        OrgEntity parent = org("parent", null);
        OrgEntity child = org("child", "parent");
        when(memberRoleRepository.findAllByTenantIdAndUserId("tenant", "user")).thenReturn(List.of(directRole));
        when(orgMemberRepository.findAllByTenantIdAndUserId("tenant", "user")).thenReturn(List.of(orgMember));
        when(orgRepository.findAllByTenantIdOrderByCreateDateTimeAsc("tenant")).thenReturn(List.of(parent, child));
        when(orgRoleRepository.findAllByTenantIdAndOrgIdIn("tenant", Set.of("child", "parent")))
            .thenReturn(List.of(orgRole("child-role"), orgRole("parent-role")));

        List<RoleEntity> roles = List.of(role("direct"), role("child-role"), role("parent-role"));
        when(roleRepository.findAllById(Set.of("direct", "child-role", "parent-role"))).thenReturn(roles);
        when(rolePermissionRepository.findAllByTenantIdAndRoleIdIn("tenant",
            Set.of("direct", "child-role", "parent-role")))
            .thenReturn(List.of(permission("workspace:datasource:view"), permission("workspace:workflow:menu"),
                permission("workspace:custom-api:execute")));

        AccessSnapshot result = productAccessService.resolve("user", "tenant");

        assertThat(result.permissions()).containsExactlyInAnyOrder("workspace:datasource:view",
            "workspace:workflow:menu", "workspace:custom-api:execute");
    }

    @Test
    void normalAdminHasAllWorkspacePermissionsWithoutRoleBindings() {

        when(userRepository.findById("user")).thenReturn(Optional.of(enabledUser()));
        when(tenantRepository.findById("tenant")).thenReturn(Optional.of(enabledTenant()));
        when(tenantUserRepository.findByTenantIdAndUserId("tenant", "user"))
            .thenReturn(Optional.of(enabledMember(true)));

        AccessSnapshot result = productAccessService.resolve("user", "tenant");

        assertThat(result.normalAdmin()).isTrue();
        assertThat(result.hasAllWorkspacePermissions()).isTrue();
        assertThat(result.permissions()).isEmpty();
    }

    @Test
    void disabledTenantCannotBeResolved() {

        TenantEntity tenant = enabledTenant();
        tenant.setStatus(TenantStatus.DISABLE);
        when(userRepository.findById("user")).thenReturn(Optional.of(enabledUser()));
        when(tenantRepository.findById("tenant")).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> productAccessService.resolve("user", "tenant")).isInstanceOf(IsxAppException.class)
            .hasMessageContaining("租户");
    }

    private UserEntity enabledUser() {

        UserEntity user = new UserEntity();
        user.setId("user");
        user.setRoleCode(RoleType.NORMAL_MEMBER);
        user.setStatus(UserStatus.ENABLE);
        user.setPlatformAdmin(false);
        return user;
    }

    private TenantEntity enabledTenant() {

        TenantEntity tenant = new TenantEntity();
        tenant.setId("tenant");
        tenant.setStatus(TenantStatus.ENABLE);
        tenant.setAdminUserId("tenant-admin");
        return tenant;
    }

    private TenantUserEntity enabledMember(boolean normalAdmin) {

        TenantUserEntity member = new TenantUserEntity();
        member.setTenantId("tenant");
        member.setUserId("user");
        member.setStatus(UserStatus.ENABLE);
        member.setNormalAdmin(normalAdmin);
        return member;
    }

    private MemberRoleEntity memberRole(String roleId) {

        MemberRoleEntity result = new MemberRoleEntity();
        result.setRoleId(roleId);
        return result;
    }

    private OrgMemberEntity orgMember(String orgId) {

        OrgMemberEntity result = new OrgMemberEntity();
        result.setOrgId(orgId);
        return result;
    }

    private OrgEntity org(String id, String parentId) {

        OrgEntity result = new OrgEntity();
        result.setId(id);
        result.setParentId(parentId);
        return result;
    }

    private OrgRoleEntity orgRole(String roleId) {

        OrgRoleEntity result = new OrgRoleEntity();
        result.setRoleId(roleId);
        return result;
    }

    private RoleEntity role(String id) {

        RoleEntity result = new RoleEntity();
        result.setId(id);
        result.setStatus(TenantStatus.ENABLE);
        return result;
    }

    private RolePermissionEntity permission(String code) {

        RolePermissionEntity result = new RolePermissionEntity();
        result.setPermissionCode(code);
        return result;
    }
}
