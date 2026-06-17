package com.isxcode.spark.modules.auth.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.Common;
import com.aliyun.teautil.models.RuntimeOptions;
import com.isxcode.spark.api.auth.constants.LoginCodeScene;
import com.isxcode.spark.api.auth.constants.LoginCodeSendStatus;
import com.isxcode.spark.api.auth.constants.LoginCodeVerifyStatus;
import com.isxcode.spark.api.auth.constants.LoginLogMethod;
import com.isxcode.spark.api.auth.constants.LoginMethodType;
import com.isxcode.spark.api.auth.dto.EmailLoginConfig;
import com.isxcode.spark.api.auth.dto.PhoneLoginConfig;
import com.isxcode.spark.api.auth.req.PageLoginCodeRecordReq;
import com.isxcode.spark.api.auth.req.SendLoginCodeReq;
import com.isxcode.spark.api.auth.req.VerifyLoginCodeReq;
import com.isxcode.spark.api.auth.res.PageLoginCodeRecordRes;
import com.isxcode.spark.api.tenant.req.AddTenantReq;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.api.user.constants.UserStatus;
import com.isxcode.spark.api.user.res.LoginRes;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.common.security.ContextHolder;
import com.isxcode.spark.modules.auth.entity.LoginCodeRecordEntity;
import com.isxcode.spark.modules.auth.repository.LoginCodeRecordRepository;
import com.isxcode.spark.modules.platform.service.PlatformSettingService;
import com.isxcode.spark.modules.tenant.service.biz.TenantBizService;
import com.isxcode.spark.modules.user.service.UserBizService;
import com.isxcode.spark.security.user.UserEntity;
import com.isxcode.spark.security.user.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class LoginMethodBizService {

    private static final int CODE_EXPIRE_MINUTES = 5;

    private static final int RESEND_SECONDS = 60;

    private static final int MAX_VERIFY_FAIL_COUNT = 5;

    private static final int TEXT_LIMIT = 1000;

    private static final String SYSTEM_USER = "system";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final LoginMethodConfigService loginMethodConfigService;

    private final LoginCodeRecordRepository loginCodeRecordRepository;

    private final UserRepository userRepository;

    private final UserBizService userBizService;

    private final LoginLogService loginLogService;

    private final PlatformSettingService platformSettingService;

    private final TenantBizService tenantBizService;

    public void sendCode(SendLoginCodeReq sendLoginCodeReq) {

        withSystemUser(() -> {
            String channel = normalizeChannel(sendLoginCodeReq.getChannel());
            String receiver = normalizeReceiver(channel, sendLoginCodeReq.getReceiver());
            LoginMethodRuntimeConfig config = loginMethodConfigService.getRuntimeConfig();
            validateChannelEnabled(channel, config);
            validateReceiverCanLoginOrRegister(channel, receiver, config);
            validateResendInterval(channel, receiver);

            String code = generateCode();
            LoginCodeRecordEntity record = new LoginCodeRecordEntity();
            record.setChannel(channel);
            record.setReceiver(receiver);
            record.setScene(LoginCodeScene.LOGIN);
            record.setCodeHash(buildCodeHash(receiver, code));
            record.setExpireDateTime(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES));
            record.setVerifyStatus(LoginCodeVerifyStatus.WAIT);
            record.setVerifyFailCount(0);
            record.setRegistered(false);
            record.setAutoTenantCreated(false);

            try {
                String providerMessage = sendCodeMessage(channel, receiver, code, config);
                record.setSendStatus(LoginCodeSendStatus.SUCCESS);
                record.setProviderMessage(truncate(providerMessage));
                loginCodeRecordRepository.save(record);
            } catch (Exception exception) {
                record.setSendStatus(LoginCodeSendStatus.FAIL);
                record.setVerifyStatus(LoginCodeVerifyStatus.FAIL);
                record.setErrorMessage(truncate(exception.getMessage()));
                loginCodeRecordRepository.save(record);
                throw new IsxAppException("验证码发送失败：" + exception.getMessage());
            }
            return null;
        });
    }

    public LoginRes verifyLogin(VerifyLoginCodeReq verifyLoginCodeReq) {

        return withSystemUser(() -> {
            String loginMethod = resolveCodeLoginLogMethod(verifyLoginCodeReq.getChannel());
            String receiver = valueOrEmpty(verifyLoginCodeReq.getReceiver()).trim();
            String userId = null;
            boolean registered = false;
            boolean autoTenantCreated = false;
            try {
                String channel = normalizeChannel(verifyLoginCodeReq.getChannel());
                loginMethod = resolveCodeLoginLogMethod(channel);
                receiver = normalizeReceiver(channel, verifyLoginCodeReq.getReceiver());
                String code = verifyLoginCodeReq.getCode().trim();
                if (!code.matches("^\\d{6}$")) {
                    throw new IsxAppException("验证码格式不正确");
                }

                LoginMethodRuntimeConfig config = loginMethodConfigService.getRuntimeConfig();
                validateChannelEnabled(channel, config);
                LoginCodeRecordEntity record = loginCodeRecordRepository
                    .findFirstByChannelAndReceiverAndSceneAndSendStatusOrderByCreateDateTimeDesc(channel, receiver,
                        LoginCodeScene.LOGIN, LoginCodeSendStatus.SUCCESS)
                    .orElseThrow(() -> new IsxAppException("请先获取验证码"));

                if (!LoginCodeVerifyStatus.WAIT.equals(record.getVerifyStatus())) {
                    throw new IsxAppException("验证码已失效，请重新获取");
                }
                if (record.getExpireDateTime() == null || LocalDateTime.now().isAfter(record.getExpireDateTime())) {
                    record.setVerifyStatus(LoginCodeVerifyStatus.EXPIRED);
                    loginCodeRecordRepository.save(record);
                    throw new IsxAppException("验证码已过期，请重新获取");
                }
                if (!buildCodeHash(receiver, code).equals(record.getCodeHash())) {
                    int failCount = record.getVerifyFailCount() == null ? 1 : record.getVerifyFailCount() + 1;
                    record.setVerifyFailCount(failCount);
                    if (failCount >= MAX_VERIFY_FAIL_COUNT) {
                        record.setVerifyStatus(LoginCodeVerifyStatus.FAIL);
                    }
                    loginCodeRecordRepository.save(record);
                    throw new IsxAppException("验证码不正确");
                }

                UserEntity user = findUser(channel, receiver).orElse(null);
                if (user == null) {
                    validateRegisterEnabled(channel, config);
                    user = createAutoRegisterUser(channel, receiver);
                    registered = true;
                    autoTenantCreated = createDefaultTenantIfEnabled(user);
                }
                userId = user.getId();

                record.setVerifyStatus(LoginCodeVerifyStatus.VERIFIED);
                record.setVerifyDateTime(LocalDateTime.now());
                record.setRegistered(registered);
                record.setAutoTenantCreated(autoTenantCreated);
                loginCodeRecordRepository.save(record);

                LoginRes loginRes = userBizService.loginAuthenticatedUser(user);
                loginLogService.recordSuccess(loginMethod, receiver, userId, registered);
                return loginRes;
            } catch (RuntimeException exception) {
                loginLogService.recordFail(loginMethod, receiver, userId, registered, exception.getMessage());
                throw exception;
            }
        });
    }

    public void testSendCode(SendLoginCodeReq sendLoginCodeReq) {

        String channel = normalizeChannel(sendLoginCodeReq.getChannel());
        String receiver = normalizeReceiver(channel, sendLoginCodeReq.getReceiver());
        LoginMethodRuntimeConfig config = loginMethodConfigService.getRuntimeConfig();
        try {
            sendCodeMessage(channel, receiver, generateCode(), config);
        } catch (Exception exception) {
            throw new IsxAppException("验证码发送失败：" + exception.getMessage());
        }
    }

    public Page<PageLoginCodeRecordRes> pageRecord(PageLoginCodeRecordReq pageLoginCodeRecordReq) {

        String searchKeyWord =
            pageLoginCodeRecordReq.getSearchKeyWord() == null ? "" : pageLoginCodeRecordReq.getSearchKeyWord();
        String channel = pageLoginCodeRecordReq.getChannel() == null ? "" : pageLoginCodeRecordReq.getChannel();
        Page<LoginCodeRecordEntity> recordPage = loginCodeRecordRepository.pageRecord(searchKeyWord, channel,
            PageRequest.of(pageLoginCodeRecordReq.getPage(), pageLoginCodeRecordReq.getPageSize()));
        return recordPage.map(this::toPageRecordRes);
    }

    private String sendCodeMessage(String channel, String receiver, String code, LoginMethodRuntimeConfig config) {

        if (LoginMethodType.EMAIL.equals(channel)) {
            return sendEmailCode(receiver, code, config.getConfig().getEmailConfig());
        }
        if (LoginMethodType.PHONE.equals(channel)) {
            return sendPhoneCode(receiver, code, config.getConfig().getPhoneConfig());
        }
        throw new IsxAppException("登录方式不支持");
    }

    private String sendEmailCode(String receiver, String code, EmailLoginConfig emailConfig) {

        validateEmailConfig(emailConfig);

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(emailConfig.getHost());
        mailSender.setPort(emailConfig.getPort());
        mailSender.setUsername(emailConfig.getUsername());
        mailSender.setPassword(emailConfig.getPassword());
        mailSender.setProtocol(JavaMailSenderImpl.DEFAULT_PROTOCOL);
        mailSender.setDefaultEncoding("UTF-8");

        mailSender.getJavaMailProperties().put("mail.smtp.auth", !Strings.isEmpty(emailConfig.getUsername()));
        mailSender.getJavaMailProperties().put("mail.smtp.starttls.enable",
            Boolean.TRUE.equals(emailConfig.getStartTls()));
        mailSender.getJavaMailProperties().put("mail.smtp.ssl.enable", Boolean.TRUE.equals(emailConfig.getSsl()));

        String fromAddress = resolveEmailFromAddress(emailConfig);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(receiver);
        message.setSubject(Strings.isEmpty(emailConfig.getSubject()) ? "至轻云登录验证码" : emailConfig.getSubject());
        message.setText("您的登录验证码为：" + code + "，" + CODE_EXPIRE_MINUTES + "分钟内有效。");
        mailSender.send(message);
        return "邮件发送成功";
    }

    private String resolveEmailFromAddress(EmailLoginConfig emailConfig) {

        if ("QQ".equalsIgnoreCase(emailConfig.getProvider())) {
            return emailConfig.getUsername();
        }
        return Strings.isEmpty(emailConfig.getFromAddress()) ? emailConfig.getUsername() : emailConfig.getFromAddress();
    }

    private String sendPhoneCode(String receiver, String code, PhoneLoginConfig phoneConfig) {

        validatePhoneConfig(phoneConfig);
        try {
            Config config = new Config().setAccessKeyId(phoneConfig.getAccessKeyId())
                .setAccessKeySecret(phoneConfig.getAccessKeySecret()).setRegionId(phoneConfig.getRegionId())
                .setEndpoint("dysmsapi.aliyuncs.com");
            Client client = new Client(config);
            JSONObject templateParam = new JSONObject();
            templateParam.put(phoneConfig.getTemplateParamName(), code);
            SendSmsRequest sendSmsRequest = new SendSmsRequest().setSignName(phoneConfig.getSignName())
                .setTemplateCode(phoneConfig.getTemplateCode()).setPhoneNumbers(receiver)
                .setTemplateParam(templateParam.toJSONString());
            SendSmsResponse sendSmsResponse = client.sendSmsWithOptions(sendSmsRequest, new RuntimeOptions());
            if (!"OK".equals(sendSmsResponse.getBody().getCode())) {
                throw new RuntimeException(sendSmsResponse.getBody().getMessage());
            }
            return JSON.toJSONString(sendSmsResponse);
        } catch (Exception exception) {
            TeaException error = new TeaException(exception.getMessage(), exception);
            throw new RuntimeException(Common.assertAsString(error.message));
        }
    }

    private void validateEmailConfig(EmailLoginConfig config) {

        if (config == null || Strings.isEmpty(config.getHost()) || config.getPort() == null
            || Strings.isEmpty(config.getUsername()) || Strings.isEmpty(config.getPassword())) {
            throw new IsxAppException("邮箱配置未完成");
        }
    }

    private void validatePhoneConfig(PhoneLoginConfig config) {

        if (config == null || !"ALIYUN".equalsIgnoreCase(config.getProvider()) || Strings.isEmpty(config.getRegionId())
            || Strings.isEmpty(config.getAccessKeyId()) || Strings.isEmpty(config.getAccessKeySecret())
            || Strings.isEmpty(config.getSignName()) || Strings.isEmpty(config.getTemplateCode())) {
            throw new IsxAppException("手机配置未完成");
        }
        if (Strings.isEmpty(config.getTemplateParamName())) {
            config.setTemplateParamName("code");
        }
    }

    private void validateChannelEnabled(String channel, LoginMethodRuntimeConfig config) {

        if (LoginMethodType.EMAIL.equals(channel) && Boolean.TRUE.equals(config.getEmailEnabled())) {
            return;
        }
        if (LoginMethodType.PHONE.equals(channel) && Boolean.TRUE.equals(config.getPhoneEnabled())) {
            return;
        }
        throw new IsxAppException("登录方式未开启");
    }

    private void validateReceiverCanLoginOrRegister(String channel, String receiver, LoginMethodRuntimeConfig config) {

        if (findUser(channel, receiver).isPresent()) {
            return;
        }
        validateRegisterEnabled(channel, config);
    }

    private void validateRegisterEnabled(String channel, LoginMethodRuntimeConfig config) {

        if (LoginMethodType.EMAIL.equals(channel) && Boolean.TRUE.equals(config.getEmailRegisterEnabled())) {
            return;
        }
        if (LoginMethodType.PHONE.equals(channel) && Boolean.TRUE.equals(config.getPhoneRegisterEnabled())) {
            return;
        }
        throw new IsxAppException("当前登录方式未开启注册");
    }

    private void validateResendInterval(String channel, String receiver) {

        Optional<LoginCodeRecordEntity> latestRecord = loginCodeRecordRepository
            .findFirstByChannelAndReceiverAndSceneOrderByCreateDateTimeDesc(channel, receiver, LoginCodeScene.LOGIN);
        if (latestRecord.isEmpty() || latestRecord.get().getCreateDateTime() == null
            || !LoginCodeSendStatus.SUCCESS.equals(latestRecord.get().getSendStatus())) {
            return;
        }
        if (latestRecord.get().getCreateDateTime().plusSeconds(RESEND_SECONDS).isAfter(LocalDateTime.now())) {
            throw new IsxAppException("验证码发送过于频繁，请稍后再试");
        }
    }

    private Optional<UserEntity> findUser(String channel, String receiver) {

        if (LoginMethodType.EMAIL.equals(channel)) {
            return userRepository.findByEmail(receiver);
        }
        if (LoginMethodType.PHONE.equals(channel)) {
            return userRepository.findByPhone(receiver);
        }
        return Optional.empty();
    }

    private UserEntity createAutoRegisterUser(String channel, String receiver) {

        UserEntity user = new UserEntity();
        String account = generateUniqueAccount();
        user.setAccount(account);
        user.setUsername("用户" + account.substring(1));
        user.setPasswd("");
        user.setRoleCode(RoleType.PLATFORM_MEMBER);
        user.setPlatformAdmin(false);
        user.setStatus(UserStatus.ENABLE);
        if (LoginMethodType.EMAIL.equals(channel)) {
            user.setEmail(receiver);
        }
        if (LoginMethodType.PHONE.equals(channel)) {
            user.setPhone(receiver);
        }
        return userRepository.save(user);
    }

    private boolean createDefaultTenantIfEnabled(UserEntity user) {

        if (!Boolean.TRUE.equals(platformSettingService.getSetting().getAutoCreateTenant())) {
            return false;
        }

        AddTenantReq addTenantReq = new AddTenantReq();
        addTenantReq.setName(buildDefaultTenantName(user));
        addTenantReq.setAdminUserId(user.getId());
        tenantBizService.addTenant(addTenantReq);
        return true;
    }

    private String buildDefaultTenantName(UserEntity user) {

        String username = Strings.isEmpty(user.getUsername()) ? user.getAccount() : user.getUsername();
        return username + "的团队";
    }

    private String generateUniqueAccount() {

        String account = "u" + IdUtil.getSnowflakeNextIdStr();
        while (userRepository.findByAccount(account).isPresent()) {
            account = "u" + IdUtil.getSnowflakeNextIdStr();
        }
        return account;
    }

    private String generateCode() {

        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private String buildCodeHash(String receiver, String code) {

        return SecureUtil.md5(receiver + ":" + code);
    }

    private String normalizeChannel(String channel) {

        String normalizedChannel = channel == null ? "" : channel.trim().toUpperCase();
        if (!LoginMethodType.EMAIL.equals(normalizedChannel) && !LoginMethodType.PHONE.equals(normalizedChannel)) {
            throw new IsxAppException("登录方式不支持");
        }
        return normalizedChannel;
    }

    private String resolveCodeLoginLogMethod(String channel) {

        String normalizedChannel = channel == null ? "" : channel.trim().toUpperCase();
        if (LoginMethodType.PHONE.equals(normalizedChannel)) {
            return LoginLogMethod.PHONE_CODE;
        }
        return LoginLogMethod.EMAIL_CODE;
    }

    private String normalizeReceiver(String channel, String receiver) {

        String normalizedReceiver = receiver == null ? "" : receiver.trim();
        if (LoginMethodType.EMAIL.equals(channel)) {
            normalizedReceiver = normalizedReceiver.toLowerCase();
            if (!EMAIL_PATTERN.matcher(normalizedReceiver).matches()) {
                throw new IsxAppException("邮箱格式不正确");
            }
        }
        if (LoginMethodType.PHONE.equals(channel) && !PHONE_PATTERN.matcher(normalizedReceiver).matches()) {
            throw new IsxAppException("手机号格式不正确");
        }
        return normalizedReceiver;
    }

    private PageLoginCodeRecordRes toPageRecordRes(LoginCodeRecordEntity record) {

        return PageLoginCodeRecordRes.builder().id(record.getId()).channel(record.getChannel())
            .receiver(maskReceiver(record.getChannel(), record.getReceiver())).scene(record.getScene())
            .sendStatus(record.getSendStatus()).verifyStatus(record.getVerifyStatus())
            .registered(record.getRegistered()).autoTenantCreated(record.getAutoTenantCreated())
            .errorMessage(record.getErrorMessage()).providerMessage(record.getProviderMessage())
            .createDateTime(record.getCreateDateTime()).verifyDateTime(record.getVerifyDateTime()).build();
    }

    private String maskReceiver(String channel, String receiver) {

        if (Strings.isEmpty(receiver)) {
            return "";
        }
        if (LoginMethodType.EMAIL.equals(channel)) {
            int atIndex = receiver.indexOf('@');
            if (atIndex <= 1) {
                return "***" + receiver.substring(Math.max(atIndex, 0));
            }
            return receiver.charAt(0) + "***" + receiver.substring(atIndex);
        }
        if (receiver.length() <= 7) {
            return receiver.substring(0, 1) + "***";
        }
        return receiver.substring(0, 3) + "****" + receiver.substring(receiver.length() - 4);
    }

    private String truncate(String value) {

        if (value == null || value.length() <= TEXT_LIMIT) {
            return value;
        }
        return value.substring(0, TEXT_LIMIT);
    }

    private String valueOrEmpty(String value) {

        return value == null ? "" : value;
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
