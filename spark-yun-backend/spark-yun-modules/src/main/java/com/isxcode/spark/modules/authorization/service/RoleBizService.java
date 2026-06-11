package com.isxcode.spark.modules.authorization.service;

import com.isxcode.spark.api.authorization.req.DeleteRoleReq;
import com.isxcode.spark.api.authorization.req.PageRoleReq;
import com.isxcode.spark.api.authorization.req.SaveRoleReq;
import com.isxcode.spark.api.authorization.res.PermissionCatalogRes;
import com.isxcode.spark.api.authorization.res.RoleRes;
import com.isxcode.spark.api.tenant.constants.TenantStatus;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.common.security.ContextHolder;
import com.isxcode.spark.security.authorization.MemberRoleRepository;
import com.isxcode.spark.security.authorization.OrgRoleRepository;
import com.isxcode.spark.security.authorization.RoleEntity;
import com.isxcode.spark.security.authorization.RolePermissionEntity;
import com.isxcode.spark.security.authorization.RolePermissionRepository;
import com.isxcode.spark.security.authorization.RoleRepository;
import com.isxcode.spark.security.authorization.WorkspacePermissionCatalog;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class RoleBizService {

    private final RoleRepository roleRepository;

    private final RolePermissionRepository rolePermissionRepository;

    private final MemberRoleRepository memberRoleRepository;

    private final OrgRoleRepository orgRoleRepository;

    public void saveRole(SaveRoleReq request) {

        String tenantId = requireTenantId();
        RoleEntity role;
        if (Strings.isEmpty(request.getId())) {
            role = new RoleEntity();
        } else {
            role = getCurrentTenantRole(request.getId());
        }
        roleRepository.findByTenantIdAndName(tenantId, request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(role.getId())) {
                throw new IsxAppException("同一租户下角色名称不能重复");
            }
        });
        roleRepository.findByTenantIdAndCode(tenantId, request.getCode()).ifPresent(existing -> {
            if (!existing.getId().equals(role.getId())) {
                throw new IsxAppException("同一租户下角色编码不能重复");
            }
        });

        role.setTenantId(tenantId);
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setStatus(Strings.isEmpty(request.getStatus()) ? TenantStatus.ENABLE : request.getStatus());
        RoleEntity savedRole = roleRepository.save(role);

        rolePermissionRepository.deleteAllByTenantIdAndRoleId(tenantId, savedRole.getId());
        Set<String> validCodes = WorkspacePermissionCatalog.allCodes();
        if (request.getPermissionCodes() != null) {
            request.getPermissionCodes().stream().distinct().filter(validCodes::contains).forEach(code -> {
                RolePermissionEntity permission = new RolePermissionEntity();
                permission.setTenantId(tenantId);
                permission.setRoleId(savedRole.getId());
                permission.setPermissionCode(code);
                rolePermissionRepository.save(permission);
            });
        }
    }

    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public Page<RoleRes> pageRole(PageRoleReq request) {

        String tenantId = requireTenantId();
        return roleRepository
            .search(tenantId, request.getSearchKeyWord(), PageRequest.of(request.getPage(), request.getPageSize()))
            .map(this::toRoleRes);
    }

    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public List<RoleRes> listRole() {

        return roleRepository.findAllByTenantIdAndStatus(requireTenantId(), TenantStatus.ENABLE).stream()
            .map(this::toRoleRes).toList();
    }

    public void deleteRole(DeleteRoleReq request) {

        String tenantId = requireTenantId();
        RoleEntity role = getCurrentTenantRole(request.getRoleId());
        if (memberRoleRepository.countByTenantIdAndRoleId(tenantId, role.getId()) > 0
            || orgRoleRepository.countByTenantIdAndRoleId(tenantId, role.getId()) > 0) {
            throw new IsxAppException("已绑定用户或组织的角色不能删除");
        }
        rolePermissionRepository.deleteAllByTenantIdAndRoleId(tenantId, role.getId());
        roleRepository.delete(role);
    }

    public PermissionCatalogRes permissionCatalog() {

        return PermissionCatalogRes.builder().modules(WorkspacePermissionCatalog.modules())
            .actions(WorkspacePermissionCatalog.actions())
            .permissionCodes(List.copyOf(WorkspacePermissionCatalog.allCodes())).build();
    }

    private RoleRes toRoleRes(RoleEntity role) {

        List<String> permissionCodes =
            rolePermissionRepository.findAllByTenantIdAndRoleId(requireTenantId(), role.getId()).stream()
                .map(RolePermissionEntity::getPermissionCode).toList();
        return RoleRes.builder().id(role.getId()).name(role.getName()).code(role.getCode()).status(role.getStatus())
            .permissionCodes(permissionCodes).build();
    }

    private RoleEntity getCurrentTenantRole(String roleId) {

        RoleEntity role = roleRepository.findById(roleId).orElseThrow(() -> new IsxAppException("角色不存在"));
        if (!requireTenantId().equals(role.getTenantId())) {
            throw new IsxAppException("无权操作其他租户角色");
        }
        return role;
    }

    private String requireTenantId() {

        if (Strings.isEmpty(ContextHolder.getTenantId())) {
            throw new IsxAppException("租户id丢失");
        }
        return ContextHolder.getTenantId();
    }
}
