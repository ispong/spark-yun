package com.isxcode.spark.security.main;

import com.isxcode.spark.common.security.ContextHolder;
import com.isxcode.spark.security.authorization.AccessSnapshot;
import com.isxcode.spark.security.authorization.ProductAccessService;
import com.isxcode.spark.security.authorization.WorkspacePermissionCatalog;
import java.io.IOException;
import java.util.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class ProductAccessAuthorizationFilter extends OncePerRequestFilter {

    private static final Set<String> PLATFORM_OLD_PATHS =
        Set.of("/user/pageUser", "/user/addUser", "/user/updateUser", "/user/updateUserPassword", "/user/disableUser",
            "/user/enableUser", "/user/deleteUser", "/user/setPlatformAdmin", "/tenant/addTenant", "/tenant/pageTenant",
            "/tenant/updateTenantForSystemAdmin", "/tenant/enableTenant", "/tenant/disableTenant",
            "/tenant/checkTenant", "/tenant/deleteTenant", "/tenant/replaceAdmin");

    private static final Set<String> ADMIN_OLD_PREFIXES = Set.of("/tenant-user/");

    private final ProductAccessService productAccessService;

    private final AccessDeniedHandler accessDeniedHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        try {
            if (isPlatformPath(path)) {
                checkPlatformAccess();
            } else if (isAdminPath(path)) {
                checkAdminAccess();
            } else {
                String module = WorkspacePermissionCatalog.resolveModule(path);
                if (module != null) {
                    checkWorkspaceAccess(module, WorkspacePermissionCatalog.resolveAction(path), request.getMethod(),
                        path);
                }
            }
            filterChain.doFilter(request, response);
        } catch (AccessDeniedException exception) {
            accessDeniedHandler.handle(request, response, exception);
        }
    }

    private void checkPlatformAccess() {

        AccessSnapshot access = productAccessService.resolve(ContextHolder.getUserId(), ContextHolder.getTenantId());
        if (!access.systemAdmin() && !access.platformAdmin()) {
            throw new AccessDeniedException("无平台管理权限");
        }
    }

    private void checkAdminAccess() {

        AccessSnapshot access = productAccessService.resolve(ContextHolder.getUserId(), ContextHolder.getTenantId());
        if (!access.platformAdmin() && !access.tenantAdmin() && !access.normalAdmin()) {
            throw new AccessDeniedException("无后台管理权限");
        }
    }

    private void checkWorkspaceAccess(String module, String action, String method, String path) {

        AccessSnapshot access = productAccessService.resolve(ContextHolder.getUserId(), ContextHolder.getTenantId());
        if (access.systemAdmin()) {
            throw new AccessDeniedException("超级管理员不能进入工作台");
        }
        if (!access.hasTenantAccess()) {
            throw new AccessDeniedException("无工作台操作权限");
        }
        if (!productAccessService.hasWorkspacePermission(access, module, action)) {
            throw new AccessDeniedException("无工作台操作权限");
        }
        if (!productAccessService.hasWorkspaceDataPermission(access, module, action)) {
            throw new AccessDeniedException("无工作台数据权限");
        }
        if (!productAccessService.hasWorkspaceApiPermission(access, module, method, path)) {
            throw new AccessDeniedException("无工作台接口权限");
        }
    }

    private boolean isPlatformPath(String path) {

        return path.startsWith("/api/platform/") || PLATFORM_OLD_PATHS.contains(path)
            || path.startsWith("/vip/license/") || path.startsWith("/vip/auth/");
    }

    private boolean isAdminPath(String path) {

        return path.startsWith("/api/admin/") || ADMIN_OLD_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {

        return request.getServletPath().contains("/open/");
    }
}
