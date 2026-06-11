package com.isxcode.spark.security.authorization;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WorkspacePermissionCatalog {

    private static final List<String> ACTIONS = List.of("menu", "view", "create", "edit", "delete", "execute");

    private static final List<String> MODULES = List.of("index", "computer-group", "datasource", "file-center",
        "data-layer", "field-format", "data-model", "workflow", "realtime-computing", "spark-container", "custom-func",
        "global-variables", "lib-package", "schedule", "message-notifications", "warning-config", "metadata-management",
        "acquisition-task", "report-views", "custom-api", "custom-form");

    private WorkspacePermissionCatalog() {}

    public static String code(String module, String action) {

        return "workspace:" + module + ":" + action;
    }

    public static Set<String> allCodes() {

        Set<String> result = new LinkedHashSet<>();
        MODULES.forEach(module -> ACTIONS.forEach(action -> result.add(code(module, action))));
        return result;
    }

    public static List<String> modules() {

        return new ArrayList<>(MODULES);
    }

    public static List<String> actions() {

        return new ArrayList<>(ACTIONS);
    }
}
