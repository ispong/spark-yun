package com.isxcode.spark.security.main;

import com.isxcode.spark.common.security.ContextHolder;
import com.isxcode.spark.security.authorization.AccessSnapshot;
import com.isxcode.spark.security.authorization.ProductAccessService;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
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

    private static final Map<String, String> WORKSPACE_MODULES = Map.ofEntries(Map.entry("/cluster/", "computer-group"),
        Map.entry("/cluster-node/", "computer-group"), Map.entry("/datasource/", "datasource"),
        Map.entry("/workflow/", "workflow"), Map.entry("/work/", "workflow"), Map.entry("/file/", "file-center"),
        Map.entry("/func/", "custom-func"), Map.entry("/alarm/", "warning-config"), Map.entry("/monitor/", "index"),
        Map.entry("/vip/work/", "workflow"), Map.entry("/vip/work-instance/", "workflow"),
        Map.entry("/vip/workflow-instance/", "workflow"), Map.entry("/vip/api-service/", "custom-api"),
        Map.entry("/vip/form/", "custom-form"), Map.entry("/vip/container/", "spark-container"),
        Map.entry("/vip/real/", "realtime-computing"), Map.entry("/vip/view/", "report-views"),
        Map.entry("/vip/meta/", "metadata-management"), Map.entry("/vip/layer/", "data-layer"),
        Map.entry("/vip/model/", "data-model"), Map.entry("/vip/secret/", "global-variables"));

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
                String module = resolveWorkspaceModule(path);
                if (module != null) {
                    checkWorkspaceAccess(module, resolveAction(path));
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

    private void checkWorkspaceAccess(String module, String action) {

        AccessSnapshot access = productAccessService.resolve(ContextHolder.getUserId(), ContextHolder.getTenantId());
        if (access.systemAdmin()) {
            throw new AccessDeniedException("超级管理员不能进入工作台");
        }
        if (!access.hasTenantAccess()) {
            throw new AccessDeniedException("无工作台操作权限");
        }
    }

    private boolean isPlatformPath(String path) {

        return path.startsWith("/api/platform/") || PLATFORM_OLD_PATHS.contains(path)
            || path.startsWith("/vip/license/") || path.startsWith("/vip/auth/");
    }

    private boolean isAdminPath(String path) {

        return path.startsWith("/api/admin/") || ADMIN_OLD_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private String resolveWorkspaceModule(String path) {

        String endpoint = endpointName(path).toLowerCase(Locale.ROOT);
        if (isModulePath(path, "alarm") && endpoint.contains("message")) {
            return "message-notifications";
        }
        if (isModulePath(path, "vip/meta")
            && (endpoint.contains("metawork") || endpoint.contains("metaworkinstance"))) {
            return "acquisition-task";
        }
        if (isModulePath(path, "vip/model") && endpoint.contains("columnformat")) {
            return "field-format";
        }
        if (isModulePath(path, "vip/work-instance") || isModulePath(path, "vip/workflow-instance")) {
            return "schedule";
        }
        if (path.startsWith("/api/workspace/")) {
            String remaining = path.substring("/api/workspace/".length());
            if (remaining.startsWith("vip/")) {
                return WORKSPACE_MODULES.entrySet().stream()
                    .filter(entry -> ("/" + remaining).startsWith(entry.getKey())).map(Map.Entry::getValue).findFirst()
                    .orElse(null);
            }
            int separator = remaining.indexOf('/');
            String modulePath = separator < 0 ? remaining : remaining.substring(0, separator);
            return WORKSPACE_MODULES.getOrDefault("/" + modulePath + "/", modulePath);
        }
        return WORKSPACE_MODULES.entrySet().stream().filter(entry -> path.startsWith(entry.getKey()))
            .map(Map.Entry::getValue).findFirst().orElse(null);
    }

    private String resolveAction(String path) {

        String action = endpointName(path).toLowerCase(Locale.ROOT);
        if (startsWithAny(action, "add", "create", "copy", "import", "upload")) {
            return "create";
        }
        if (startsWithAny(action, "update", "edit", "config", "setting", "rename", "top", "save", "set", "enable",
            "disable", "reset")) {
            return "edit";
        }
        if (startsWithAny(action, "delete", "remove")) {
            return "delete";
        }
        if (startsWithAny(action, "run", "start", "stop", "pause", "resume", "deploy", "offline", "kill", "abort",
            "break", "rerun", "invoke", "test", "check", "trigger", "fasttrigger", "publish", "build", "refresh",
            "install", "clean", "generate")) {
            return "execute";
        }
        return "view";
    }

    private String endpointName(String path) {

        return path.substring(path.lastIndexOf('/') + 1);
    }

    private boolean isModulePath(String path, String modulePath) {

        return path.startsWith("/" + modulePath + "/") || path.startsWith("/api/workspace/" + modulePath + "/");
    }

    private boolean startsWithAny(String value, String... prefixes) {

        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {

        return request.getServletPath().contains("/open/");
    }
}
