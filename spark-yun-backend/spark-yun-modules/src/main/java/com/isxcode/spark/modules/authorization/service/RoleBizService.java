package com.isxcode.spark.modules.authorization.service;

import com.isxcode.spark.api.authorization.req.DeleteRoleReq;
import com.isxcode.spark.api.authorization.req.PageRoleReq;
import com.isxcode.spark.api.authorization.req.SaveRoleReq;
import com.isxcode.spark.api.authorization.res.PermissionCatalogRes;
import com.isxcode.spark.api.authorization.res.PermissionCatalogRes.PermissionItemRes;
import com.isxcode.spark.api.authorization.res.PermissionCatalogRes.PermissionModuleRes;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class RoleBizService {

    private final RoleRepository roleRepository;

    private final RolePermissionRepository rolePermissionRepository;

    private final MemberRoleRepository memberRoleRepository;

    private final OrgRoleRepository orgRoleRepository;

    private final ObjectProvider<RequestMappingHandlerMapping> requestMappingHandlerMappingProvider;

    public void saveRole(SaveRoleReq request) {

        String tenantId = requireTenantId();
        RoleEntity role;
        boolean creating = Strings.isEmpty(request.getId());
        if (creating) {
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
        role.setRemark(request.getRemark());
        role.setStatus(Strings.isEmpty(request.getStatus()) ? TenantStatus.ENABLE : request.getStatus());
        RoleEntity savedRole = roleRepository.save(role);

        if (creating || request.getPermissionCodes() != null) {
            rolePermissionRepository.deleteAllByTenantIdAndRoleId(tenantId, savedRole.getId());
            Set<String> validCodes = validPermissionCodes();
            List<String> requestedCodes =
                request.getPermissionCodes() == null ? List.copyOf(validCodes) : request.getPermissionCodes();
            requestedCodes.stream().distinct().filter(validCodes::contains).forEach(code -> {
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

        List<PermissionModuleRes> interfacePermissions = interfacePermissions();
        return PermissionCatalogRes.builder().modules(WorkspacePermissionCatalog.modules())
            .actions(WorkspacePermissionCatalog.actions()).permissionCodes(List.copyOf(validPermissionCodes()))
            .menuPermissions(modulePermissions(List.of("menu"), false))
            .buttonPermissions(modulePermissions(WorkspacePermissionCatalog.buttonActions(), false))
            .interfacePermissions(interfacePermissions)
            .dataPermissions(modulePermissions(WorkspacePermissionCatalog.dataActions(), true)).build();
    }

    private RoleRes toRoleRes(RoleEntity role) {

        List<String> permissionCodes =
            rolePermissionRepository.findAllByTenantIdAndRoleId(requireTenantId(), role.getId()).stream()
                .map(RolePermissionEntity::getPermissionCode).toList();
        return RoleRes.builder().id(role.getId()).name(role.getName()).code(role.getCode()).remark(role.getRemark())
            .status(role.getStatus()).permissionCodes(permissionCodes).build();
    }

    private Set<String> validPermissionCodes() {

        Set<String> result = new LinkedHashSet<>(WorkspacePermissionCatalog.allCodes());
        interfacePermissions().forEach(module -> module.getPermissions()
            .forEach(permission -> result.add(permission.getPermissionCode())));
        return result;
    }

    private List<PermissionModuleRes> modulePermissions(List<String> actions, boolean dataPermission) {

        return WorkspacePermissionCatalog.modules().stream()
            .map(module -> PermissionModuleRes.builder().code(module).name(WorkspacePermissionCatalog.moduleName(module))
                .permissions(actions.stream()
                    .map(action -> PermissionItemRes.builder().code(action).name(action).action(action)
                        .permissionCode(dataPermission ? WorkspacePermissionCatalog.dataCode(module, action)
                            : WorkspacePermissionCatalog.code(module, action))
                        .build())
                    .toList())
                .build())
            .toList();
    }

    private List<PermissionModuleRes> interfacePermissions() {

        Map<String, List<PermissionItemRes>> groupedPermissions = new LinkedHashMap<>();
        WorkspacePermissionCatalog.modules().forEach(module -> groupedPermissions.put(module, new ArrayList<>()));
        RequestMappingHandlerMapping requestMappingHandlerMapping = requestMappingHandlerMappingProvider.getIfAvailable();
        if (requestMappingHandlerMapping == null) {
            return List.of();
        }

        requestMappingHandlerMapping.getHandlerMethods().keySet().forEach(mappingInfo -> mappingPaths(mappingInfo)
            .forEach(path -> {
                String module = WorkspacePermissionCatalog.resolveModule(path);
                if (module == null || !groupedPermissions.containsKey(module)) {
                    return;
                }
                List<String> methods = mappingMethods(mappingInfo);
                methods.forEach(method -> groupedPermissions.get(module)
                    .add(PermissionItemRes.builder().code(method + " " + path).name(method + " " + path)
                        .method(method).path(path).action(WorkspacePermissionCatalog.resolveAction(path))
                        .permissionCode(WorkspacePermissionCatalog.apiCode(module, method, path)).build()));
            }));

        return groupedPermissions.entrySet().stream().filter(entry -> !entry.getValue().isEmpty())
            .map(entry -> PermissionModuleRes.builder().code(entry.getKey())
                .name(WorkspacePermissionCatalog.moduleName(entry.getKey())).permissions(entry.getValue().stream()
                    .sorted((left, right) -> left.getCode().compareTo(right.getCode())).toList())
                .build())
            .toList();
    }

    private Set<String> mappingPaths(RequestMappingInfo mappingInfo) {

        if (mappingInfo.getPathPatternsCondition() != null) {
            return mappingInfo.getPathPatternsCondition().getPatternValues();
        }
        if (mappingInfo.getPatternsCondition() != null) {
            return mappingInfo.getPatternsCondition().getPatterns();
        }
        return Set.of();
    }

    private List<String> mappingMethods(RequestMappingInfo mappingInfo) {

        Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();
        if (methods.isEmpty()) {
            return List.of("GET", "POST", "PUT", "DELETE");
        }
        return methods.stream().map(RequestMethod::name).sorted().toList();
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
