package com.isxcode.spark.modules.auth.service;

import com.alibaba.fastjson.JSON;
import com.isxcode.spark.api.auth.constants.LoginMethodType;
import com.isxcode.spark.api.auth.dto.EmailLoginConfig;
import com.isxcode.spark.api.auth.dto.LoginMethodConfigDto;
import com.isxcode.spark.api.auth.dto.PhoneLoginConfig;
import com.isxcode.spark.api.auth.req.UpdateLoginMethodConfigReq;
import com.isxcode.spark.api.auth.res.GetLoginMethodConfigRes;
import com.isxcode.spark.api.auth.res.GetOpenLoginMethodConfigRes;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.common.security.ContextHolder;
import com.isxcode.spark.modules.auth.entity.LoginMethodConfigEntity;
import com.isxcode.spark.modules.auth.repository.LoginMethodConfigRepository;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class LoginMethodConfigService {

    public static final String GLOBAL_CONFIG_KEY = "GLOBAL";

    public static final String SECRET_MASK = "******";

    private static final String SYSTEM_USER = "system";

    private final LoginMethodConfigRepository loginMethodConfigRepository;

    public GetLoginMethodConfigRes getConfig() {

        return toConfigRes(getOrCreateConfig(), true);
    }

    public GetOpenLoginMethodConfigRes getOpenConfig() {

        LoginMethodRuntimeConfig runtimeConfig = getRuntimeConfig();
        return GetOpenLoginMethodConfigRes.builder().defaultLoginMethod(runtimeConfig.getDefaultLoginMethod())
            .accountEnabled(runtimeConfig.getAccountEnabled())
            .accountPhonePasswordEnabled(runtimeConfig.getAccountPhonePasswordEnabled())
            .accountEmailPasswordEnabled(runtimeConfig.getAccountEmailPasswordEnabled())
            .emailEnabled(runtimeConfig.getEmailEnabled()).emailRegisterEnabled(runtimeConfig.getEmailRegisterEnabled())
            .phoneEnabled(runtimeConfig.getPhoneEnabled()).phoneRegisterEnabled(runtimeConfig.getPhoneRegisterEnabled())
            .build();
    }

    public void updateConfig(UpdateLoginMethodConfigReq updateLoginMethodConfigReq) {

        LoginMethodConfigEntity config = getOrCreateConfig();
        LoginMethodRuntimeConfig oldConfig = toRuntimeConfig(config);

        boolean accountEnabled = valueOrDefault(updateLoginMethodConfigReq.getAccountEnabled(),
            oldConfig.getAccountEnabled());
        boolean emailEnabled =
            valueOrDefault(updateLoginMethodConfigReq.getEmailEnabled(), oldConfig.getEmailEnabled());
        boolean phoneEnabled =
            valueOrDefault(updateLoginMethodConfigReq.getPhoneEnabled(), oldConfig.getPhoneEnabled());
        if (!accountEnabled && !emailEnabled && !phoneEnabled) {
            throw new IsxAppException("至少需要开启一种登录方式");
        }
        String defaultLoginMethod = resolveDefaultLoginMethod(
            valueOrDefault(updateLoginMethodConfigReq.getDefaultLoginMethod(), oldConfig.getDefaultLoginMethod()),
            accountEnabled, emailEnabled, phoneEnabled);

        config.setDefaultLoginMethod(defaultLoginMethod);
        config.setAccountEnabled(accountEnabled);
        config.setAccountPhonePasswordEnabled(valueOrDefault(
            updateLoginMethodConfigReq.getAccountPhonePasswordEnabled(), oldConfig.getAccountPhonePasswordEnabled()));
        config.setAccountEmailPasswordEnabled(valueOrDefault(
            updateLoginMethodConfigReq.getAccountEmailPasswordEnabled(), oldConfig.getAccountEmailPasswordEnabled()));
        config.setEmailEnabled(emailEnabled);
        config.setEmailRegisterEnabled(valueOrDefault(updateLoginMethodConfigReq.getEmailRegisterEnabled(),
            oldConfig.getEmailRegisterEnabled()));
        config.setPhoneEnabled(phoneEnabled);
        config.setPhoneRegisterEnabled(valueOrDefault(updateLoginMethodConfigReq.getPhoneRegisterEnabled(),
            oldConfig.getPhoneRegisterEnabled()));
        config.setAutoCreateTenant(valueOrDefault(updateLoginMethodConfigReq.getAutoCreateTenant(),
            oldConfig.getAutoCreateTenant()));

        LoginMethodConfigDto nextConfig =
            mergeSecretConfig(oldConfig.getConfig(), updateLoginMethodConfigReq.getConfig());
        config.setConfigJson(JSON.toJSONString(nextConfig));
        loginMethodConfigRepository.save(config);
    }

    public LoginMethodRuntimeConfig getRuntimeConfig() {

        return toRuntimeConfig(loginMethodConfigRepository.findByConfigKey(GLOBAL_CONFIG_KEY)
            .orElseGet(this::defaultConfigEntity));
    }

    private LoginMethodConfigEntity getOrCreateConfig() {

        return loginMethodConfigRepository.findByConfigKey(GLOBAL_CONFIG_KEY)
            .orElseGet(() -> withSystemUser(() -> loginMethodConfigRepository.save(defaultConfigEntity())));
    }

    private LoginMethodConfigEntity defaultConfigEntity() {

        LoginMethodConfigEntity config = new LoginMethodConfigEntity();
        config.setConfigKey(GLOBAL_CONFIG_KEY);
        config.setDefaultLoginMethod(LoginMethodType.ACCOUNT);
        config.setAccountEnabled(true);
        config.setAccountPhonePasswordEnabled(false);
        config.setAccountEmailPasswordEnabled(false);
        config.setEmailEnabled(false);
        config.setEmailRegisterEnabled(false);
        config.setPhoneEnabled(false);
        config.setPhoneRegisterEnabled(false);
        config.setAutoCreateTenant(true);
        config.setConfigJson(JSON.toJSONString(defaultConfigDto()));
        return config;
    }

    private LoginMethodConfigDto defaultConfigDto() {

        LoginMethodConfigDto config = new LoginMethodConfigDto();
        config.setEmailConfig(new EmailLoginConfig());
        PhoneLoginConfig phoneConfig = new PhoneLoginConfig();
        phoneConfig.setProvider("ALIYUN");
        phoneConfig.setRegionId("cn-hangzhou");
        phoneConfig.setTemplateParamName("code");
        config.setPhoneConfig(phoneConfig);
        return config;
    }

    private LoginMethodRuntimeConfig toRuntimeConfig(LoginMethodConfigEntity config) {

        LoginMethodConfigDto configDto = parseConfig(config.getConfigJson());
        boolean accountEnabled = valueOrDefault(config.getAccountEnabled(), true);
        boolean emailEnabled = valueOrDefault(config.getEmailEnabled(), false);
        boolean phoneEnabled = valueOrDefault(config.getPhoneEnabled(), false);
        return LoginMethodRuntimeConfig.builder()
            .defaultLoginMethod(resolveDefaultLoginMethod(config.getDefaultLoginMethod(), accountEnabled, emailEnabled,
                phoneEnabled))
            .accountEnabled(accountEnabled)
            .accountPhonePasswordEnabled(valueOrDefault(config.getAccountPhonePasswordEnabled(), false))
            .accountEmailPasswordEnabled(valueOrDefault(config.getAccountEmailPasswordEnabled(), false))
            .emailEnabled(emailEnabled)
            .emailRegisterEnabled(valueOrDefault(config.getEmailRegisterEnabled(), false))
            .phoneEnabled(phoneEnabled)
            .phoneRegisterEnabled(valueOrDefault(config.getPhoneRegisterEnabled(), false))
            .autoCreateTenant(valueOrDefault(config.getAutoCreateTenant(), true)).config(configDto).build();
    }

    private GetLoginMethodConfigRes toConfigRes(LoginMethodConfigEntity entity, boolean maskSecret) {

        LoginMethodRuntimeConfig runtimeConfig = toRuntimeConfig(entity);
        LoginMethodConfigDto config = cloneConfig(runtimeConfig.getConfig());
        if (maskSecret) {
            maskSecret(config);
        }
        return GetLoginMethodConfigRes.builder().defaultLoginMethod(runtimeConfig.getDefaultLoginMethod())
            .accountEnabled(runtimeConfig.getAccountEnabled())
            .accountPhonePasswordEnabled(runtimeConfig.getAccountPhonePasswordEnabled())
            .accountEmailPasswordEnabled(runtimeConfig.getAccountEmailPasswordEnabled())
            .emailEnabled(runtimeConfig.getEmailEnabled()).emailRegisterEnabled(runtimeConfig.getEmailRegisterEnabled())
            .phoneEnabled(runtimeConfig.getPhoneEnabled()).phoneRegisterEnabled(runtimeConfig.getPhoneRegisterEnabled())
            .autoCreateTenant(runtimeConfig.getAutoCreateTenant()).config(config).build();
    }

    private LoginMethodConfigDto parseConfig(String configJson) {

        if (Strings.isEmpty(configJson)) {
            return defaultConfigDto();
        }
        LoginMethodConfigDto config = JSON.parseObject(configJson, LoginMethodConfigDto.class);
        if (config == null) {
            return defaultConfigDto();
        }
        if (config.getEmailConfig() == null) {
            config.setEmailConfig(new EmailLoginConfig());
        }
        if (config.getPhoneConfig() == null) {
            PhoneLoginConfig phoneConfig = new PhoneLoginConfig();
            phoneConfig.setProvider("ALIYUN");
            phoneConfig.setRegionId("cn-hangzhou");
            phoneConfig.setTemplateParamName("code");
            config.setPhoneConfig(phoneConfig);
        }
        if (Strings.isEmpty(config.getPhoneConfig().getProvider())) {
            config.getPhoneConfig().setProvider("ALIYUN");
        }
        if (Strings.isEmpty(config.getPhoneConfig().getTemplateParamName())) {
            config.getPhoneConfig().setTemplateParamName("code");
        }
        return config;
    }

    private LoginMethodConfigDto mergeSecretConfig(LoginMethodConfigDto oldConfig, LoginMethodConfigDto updateConfig) {

        LoginMethodConfigDto nextConfig = updateConfig == null ? cloneConfig(oldConfig) : cloneConfig(updateConfig);
        if (nextConfig.getEmailConfig() == null) {
            nextConfig.setEmailConfig(new EmailLoginConfig());
        }
        if (nextConfig.getPhoneConfig() == null) {
            nextConfig.setPhoneConfig(new PhoneLoginConfig());
        }

        EmailLoginConfig oldEmailConfig = oldConfig == null ? null : oldConfig.getEmailConfig();
        if (oldEmailConfig != null && isMaskedOrEmpty(nextConfig.getEmailConfig().getPassword())) {
            nextConfig.getEmailConfig().setPassword(oldEmailConfig.getPassword());
        }

        PhoneLoginConfig oldPhoneConfig = oldConfig == null ? null : oldConfig.getPhoneConfig();
        if (oldPhoneConfig != null && isMaskedOrEmpty(nextConfig.getPhoneConfig().getAccessKeySecret())) {
            nextConfig.getPhoneConfig().setAccessKeySecret(oldPhoneConfig.getAccessKeySecret());
        }
        if (Strings.isEmpty(nextConfig.getPhoneConfig().getProvider())) {
            nextConfig.getPhoneConfig().setProvider("ALIYUN");
        }
        if (Strings.isEmpty(nextConfig.getPhoneConfig().getTemplateParamName())) {
            nextConfig.getPhoneConfig().setTemplateParamName("code");
        }
        return nextConfig;
    }

    private LoginMethodConfigDto cloneConfig(LoginMethodConfigDto config) {

        if (config == null) {
            return defaultConfigDto();
        }
        return JSON.parseObject(JSON.toJSONString(config), LoginMethodConfigDto.class);
    }

    private void maskSecret(LoginMethodConfigDto config) {

        if (config.getEmailConfig() != null && !Strings.isEmpty(config.getEmailConfig().getPassword())) {
            config.getEmailConfig().setPassword(SECRET_MASK);
        }
        if (config.getPhoneConfig() != null && !Strings.isEmpty(config.getPhoneConfig().getAccessKeySecret())) {
            config.getPhoneConfig().setAccessKeySecret(SECRET_MASK);
        }
    }

    private boolean isMaskedOrEmpty(String value) {

        return Strings.isEmpty(value) || SECRET_MASK.equals(value);
    }

    private String resolveDefaultLoginMethod(String defaultLoginMethod, boolean accountEnabled, boolean emailEnabled,
        boolean phoneEnabled) {

        if (!Strings.isEmpty(defaultLoginMethod) && !LoginMethodType.ACCOUNT.equals(defaultLoginMethod)
            && !LoginMethodType.EMAIL.equals(defaultLoginMethod) && !LoginMethodType.PHONE.equals(defaultLoginMethod)) {
            throw new IsxAppException("默认登录方式不支持");
        }
        if (LoginMethodType.ACCOUNT.equals(defaultLoginMethod) && accountEnabled) {
            return LoginMethodType.ACCOUNT;
        }
        if (LoginMethodType.PHONE.equals(defaultLoginMethod) && phoneEnabled) {
            return LoginMethodType.PHONE;
        }
        if (LoginMethodType.EMAIL.equals(defaultLoginMethod) && emailEnabled) {
            return LoginMethodType.EMAIL;
        }
        if (accountEnabled) {
            return LoginMethodType.ACCOUNT;
        }
        if (phoneEnabled) {
            return LoginMethodType.PHONE;
        }
        if (emailEnabled) {
            return LoginMethodType.EMAIL;
        }
        throw new IsxAppException("至少需要开启一种登录方式");
    }

    private boolean valueOrDefault(Boolean value, boolean defaultValue) {

        return value == null ? defaultValue : value;
    }

    private String valueOrDefault(String value, String defaultValue) {

        return Strings.isEmpty(value) ? defaultValue : value;
    }

    private <T> T withSystemUser(Supplier<T> supplier) {

        boolean needsContext = Strings.isEmpty(ContextHolder.getUserId());
        if (needsContext) {
            ContextHolder.setCurrentUser(SYSTEM_USER, null);
        }
        try {
            return supplier.get();
        } finally {
            if (needsContext) {
                ContextHolder.clear();
            }
        }
    }
}
