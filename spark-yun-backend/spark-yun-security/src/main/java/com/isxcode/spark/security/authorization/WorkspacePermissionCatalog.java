package com.isxcode.spark.security.authorization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class WorkspacePermissionCatalog {

    private static final List<String> ACTIONS = List.of("menu", "view", "create", "edit", "delete", "execute");

    private static final List<String> BUTTON_ACTIONS = List.of("view", "create", "edit", "delete", "execute");

    private static final List<String> DATA_ACTIONS = List.of("read", "create", "update", "delete");

    private static final Map<String, String> MODULES = new LinkedHashMap<>();

    private static final Map<String, String> WORKSPACE_MODULES = Map.ofEntries(Map.entry("/cluster/", "computer-group"),
        Map.entry("/cluster-node/", "computer-group"), Map.entry("/datasource/", "datasource"),
        Map.entry("/workflow/", "workflow"), Map.entry("/work/", "workflow"), Map.entry("/file/", "file-center"),
        Map.entry("/func/", "custom-func"), Map.entry("/alarm/", "warning-config"), Map.entry("/monitor/", "index"),
        Map.entry("/vip/work/", "workflow"), Map.entry("/vip/work-instance/", "workflow"),
        Map.entry("/vip/workflow-instance/", "workflow"), Map.entry("/vip/api-service/", "custom-api"),
        Map.entry("/vip/form/", "custom-form"), Map.entry("/vip/container/", "spark-container"),
        Map.entry("/vip/real/", "realtime-computing"), Map.entry("/vip/view/", "report-views"),
        Map.entry("/vip/meta/", "metadata-management"), Map.entry("/vip/layer/", "data-layer"),
        Map.entry("/vip/model/", "data-model"), Map.entry("/vip/secret/", "global-variables"),
        Map.entry("/ai/", "zhiqing-ai"));

    static {
        MODULES.put("zhiqing-ai", "至轻智能");
        MODULES.put("index", "首页");
        MODULES.put("computer-group", "计算集群");
        MODULES.put("datasource", "数据源");
        MODULES.put("file-center", "资源中心");
        MODULES.put("data-layer", "数据分层");
        MODULES.put("field-format", "字段标准");
        MODULES.put("data-model", "数据模型");
        MODULES.put("workflow", "作业流");
        MODULES.put("realtime-computing", "实时计算");
        MODULES.put("spark-container", "计算容器");
        MODULES.put("custom-func", "函数仓库");
        MODULES.put("global-variables", "全局变量");
        MODULES.put("lib-package", "依赖合集");
        MODULES.put("schedule", "调度历史");
        MODULES.put("message-notifications", "通知管理");
        MODULES.put("warning-config", "告警配置");
        MODULES.put("metadata-management", "元数据管理");
        MODULES.put("acquisition-task", "采集任务");
        MODULES.put("report-views", "数据报表");
        MODULES.put("custom-api", "数据接口");
        MODULES.put("custom-form", "数据表单");
    }

    private WorkspacePermissionCatalog() {}

    public static String code(String module, String action) {

        return "workspace:" + module + ":" + action;
    }

    public static String dataCode(String module, String action) {

        return "workspace:" + module + ":data:" + action;
    }

    public static String apiCode(String module, String method, String path) {

        return "workspace:" + module + ":api:" + method.toUpperCase(Locale.ROOT) + ":" + path;
    }

    public static Set<String> allCodes() {

        Set<String> result = new LinkedHashSet<>();
        modules().forEach(module -> {
            ACTIONS.forEach(action -> result.add(code(module, action)));
            DATA_ACTIONS.forEach(action -> result.add(dataCode(module, action)));
        });
        return result;
    }

    public static List<String> modules() {

        return new ArrayList<>(MODULES.keySet());
    }

    public static String moduleName(String module) {

        return MODULES.getOrDefault(module, module);
    }

    public static List<String> actions() {

        return new ArrayList<>(ACTIONS);
    }

    public static List<String> buttonActions() {

        return new ArrayList<>(BUTTON_ACTIONS);
    }

    public static List<String> dataActions() {

        return new ArrayList<>(DATA_ACTIONS);
    }

    public static String resolveModule(String path) {

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

    public static String resolveAction(String path) {

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

    public static boolean hasApiPermissions(Set<String> permissions) {

        return permissions.stream().anyMatch(permission -> permission.contains(":api:"));
    }

    public static boolean hasDataPermissions(Set<String> permissions) {

        return permissions.stream().anyMatch(permission -> permission.contains(":data:"));
    }

    private static String endpointName(String path) {

        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static boolean isModulePath(String path, String modulePath) {

        return path.startsWith("/" + modulePath + "/") || path.startsWith("/api/workspace/" + modulePath + "/");
    }

    private static boolean startsWithAny(String value, String... prefixes) {

        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
