package com.isxcode.spark.common.userlog;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UserLogDictionary {

    private static final String UNKNOWN = "UNKNOWN";

    private static final List<UserLogDefinition> DEFINITIONS =
        List.of(definition(UserLogType.PLATFORM, "USER", "用户中心", "ADD", "新增", "新增用户"),
            definition(UserLogType.PLATFORM, "USER", "用户中心", "UPDATE", "编辑", "编辑用户"),
            definition(UserLogType.PLATFORM, "USER", "用户中心", "DELETE", "删除", "删除用户"),
            definition(UserLogType.PLATFORM, "USER", "用户中心", "ENABLE", "启用", "启用用户"),
            definition(UserLogType.PLATFORM, "USER", "用户中心", "DISABLE", "禁用", "禁用用户"),
            definition(UserLogType.PLATFORM, "USER", "用户中心", "SET_PLATFORM_ADMIN", "设置平台管理员", "设置平台管理员"),
            definition(UserLogType.PLATFORM, "TENANT", "租户管理", "ADD", "新增", "新增租户"),
            definition(UserLogType.PLATFORM, "TENANT", "租户管理", "UPDATE", "编辑", "编辑租户"),
            definition(UserLogType.PLATFORM, "TENANT", "租户管理", "DELETE", "删除", "删除租户"),
            definition(UserLogType.PLATFORM, "TENANT", "租户管理", "ENABLE", "启用", "启用租户"),
            definition(UserLogType.PLATFORM, "TENANT", "租户管理", "DISABLE", "禁用", "禁用租户"),
            definition(UserLogType.PLATFORM, "TENANT", "租户管理", "REPLACE_ADMIN", "替换管理员", "替换租户管理员"),
            definition(UserLogType.TENANT, "TENANT_USER", "租户成员", "ADD", "添加", "添加租户成员"),
            definition(UserLogType.TENANT, "TENANT_USER", "租户成员", "REMOVE", "移除", "移除租户成员"),
            definition(UserLogType.TENANT, "TENANT_USER", "租户成员", "SET_ADMIN", "设置管理员", "设置租户管理员"),
            definition(UserLogType.TENANT, "TENANT_USER", "租户成员", "REMOVE_ADMIN", "移除管理员", "移除租户管理员"),
            definition(UserLogType.TENANT, "TENANT_USER", "租户成员", "SET_STATUS", "设置状态", "设置成员状态"),
            definition(UserLogType.TENANT, "TENANT_USER", "租户成员", "SET_ROLES", "设置角色", "设置成员角色"),
            definition(UserLogType.TENANT, "TENANT_USER", "租户成员", "APPROVE_APPLY", "通过申请", "通过租户申请"),
            definition(UserLogType.TENANT, "TENANT_USER", "租户成员", "REJECT_APPLY", "拒绝申请", "拒绝租户申请"),
            definition(UserLogType.TENANT, "ROLE", "角色管理", "SAVE", "保存", "保存角色"),
            definition(UserLogType.TENANT, "ROLE", "角色管理", "DELETE", "删除", "删除角色"),
            definition(UserLogType.TENANT, "ROLE", "角色管理", "SAVE_PERMISSION", "保存权限", "保存角色权限"),
            definition(UserLogType.TENANT, "ORG", "成员管理", "SAVE", "保存", "保存组织"),
            definition(UserLogType.TENANT, "ORG", "成员管理", "DELETE", "删除", "删除组织"),
            definition(UserLogType.TENANT, "AI_CONFIG", "智能配置", "SAVE", "保存", "保存智能配置"),
            definition(UserLogType.TENANT, "AI_CONFIG", "智能配置", "DELETE", "删除", "删除智能配置"),
            definition(UserLogType.PLATFORM, "PLATFORM_SETTING", "平台设置", "UPDATE", "更新", "更新平台设置"),
            definition(UserLogType.PLATFORM, "PLATFORM_SETTING", "平台设置", "UPLOAD_BRAND_IMAGE", "上传品牌图片", "上传品牌图片"));

    private static final Map<String, UserLogDefinition> DEFINITION_MAP = DEFINITIONS.stream()
        .collect(Collectors.toMap(definition -> key(definition.getModuleCode(), definition.getActionCode()),
            Function.identity(), (left, right) -> left));

    private UserLogDictionary() {}

    public static UserLogDefinition resolve(String moduleCode, String actionCode, String path) {

        UserLogDefinition definition = DEFINITION_MAP.get(key(moduleCode, actionCode));
        if (definition != null) {
            return definition;
        }

        String resolvedModuleCode = valueOrDefault(moduleCode, UNKNOWN);
        String resolvedActionCode = valueOrDefault(actionCode, UNKNOWN);
        return UserLogDefinition.builder().moduleCode(resolvedModuleCode).moduleName(resolvedModuleCode)
            .logType(UserLogType.TENANT).actionCode(resolvedActionCode).actionName(resolvedActionCode)
            .apiName(valueOrDefault(path, resolvedActionCode)).build();
    }

    public static List<UserLogDefinition> definitions() {

        return DEFINITIONS;
    }

    public static List<UserLogDefinition> modules() {

        return DEFINITIONS.stream()
            .collect(Collectors.toMap(UserLogDefinition::getModuleCode, Function.identity(), (left, right) -> left))
            .values().stream().toList();
    }

    private static UserLogDefinition definition(String logType, String moduleCode, String moduleName, String actionCode,
        String actionName, String apiName) {

        return UserLogDefinition.builder().logType(logType).moduleCode(moduleCode).moduleName(moduleName)
            .actionCode(actionCode).actionName(actionName).apiName(apiName).build();
    }

    private static String key(String moduleCode, String actionCode) {

        return valueOrDefault(moduleCode, UNKNOWN) + ":" + valueOrDefault(actionCode, UNKNOWN);
    }

    private static String valueOrDefault(String value, String defaultValue) {

        return Optional.ofNullable(value).filter(item -> !Objects.equals("", item.trim())).map(String::trim)
            .orElse(defaultValue);
    }
}
