package com.isxcode.spark.security.main;

import com.isxcode.spark.common.jpa.DataScopeContext;
import com.isxcode.spark.common.security.ContextHolder;
import com.isxcode.spark.security.authorization.AccessSnapshot;
import com.isxcode.spark.security.authorization.ProductAccessService;
import com.isxcode.spark.security.authorization.WorkspacePermissionCatalog;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class ProductAccessAuthorizationFilter extends OncePerRequestFilter {

    private static final Set<String> PLATFORM_OLD_PATHS =
        Set.of("/user/pageUser", "/user/addUser", "/user/updateUser", "/user/updateUserPassword", "/user/disableUser",
            "/user/enableUser", "/user/deleteUser", "/user/setPlatformAdmin", "/tenant/addTenant", "/tenant/pageTenant",
            "/tenant/updateTenantForSystemAdmin", "/tenant/enableTenant", "/tenant/disableTenant",
            "/tenant/checkTenant", "/tenant/deleteTenant", "/tenant/replaceAdmin");

    private static final Set<String> ADMIN_OLD_PREFIXES = Set.of("/tenant-user/");

    private static final Set<String> ADMIN_OLD_PUBLIC_PATHS = Set.of("/tenant-user/applyInviteCode");

    private final ProductAccessService productAccessService;

    private final AccessDeniedHandler accessDeniedHandler;

    private final List<String> excludeUrlPaths;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        try {
            AccessSnapshot access =
                productAccessService.resolve(ContextHolder.getUserId(), ContextHolder.getTenantId());
            DataScopeContext.runWithDataScope(access.dataScope(), () -> {
                if (isPlatformPath(path)) {
                    checkPlatformAccess(access);
                } else if (isAdminPath(path)) {
                    checkAdminAccess(access);
                } else {
                    String module = WorkspacePermissionCatalog.resolveModule(path);
                    if (module != null) {
                        checkWorkspaceAccess(access, module, WorkspacePermissionCatalog.resolveAction(path),
                            request.getMethod(), path);
                    }
                }
                try {
                    filterChain.doFilter(request, response);
                } catch (IOException | ServletException exception) {
                    throw new FilterChainException(exception);
                }
            });
        } catch (AccessDeniedException exception) {
            accessDeniedHandler.handle(request, response, exception);
        } catch (FilterChainException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            if (cause instanceof ServletException servletException) {
                throw servletException;
            }
            throw exception;
        }
    }

    private void checkPlatformAccess(AccessSnapshot access) {

        if (!access.systemAdmin() && !access.platformAdmin()) {
            throw new AccessDeniedException("无平台管理权限");
        }
    }

    private void checkAdminAccess(AccessSnapshot access) {

        if (!access.platformAdmin() && !access.tenantAdmin() && !access.normalAdmin()) {
            throw new AccessDeniedException("无后台管理权限");
        }
    }

    private void checkWorkspaceAccess(AccessSnapshot access, String module, String action, String method, String path) {

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

    boolean isAdminPath(String path) {

        return !ADMIN_OLD_PUBLIC_PATHS.contains(path)
            && (path.startsWith("/api/admin/") || ADMIN_OLD_PREFIXES.stream().anyMatch(path::startsWith));
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {

        return true;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {

        return true;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {

        String path = request.getServletPath();
        return path.contains("/open/") || excludeUrlPaths.stream().anyMatch(p -> antPathMatcher.match(p, path));
    }

    private static class FilterChainException extends RuntimeException {

        private FilterChainException(Throwable cause) {

            super(cause);
        }
    }
}
