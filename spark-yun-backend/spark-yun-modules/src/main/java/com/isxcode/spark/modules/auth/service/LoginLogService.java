package com.isxcode.spark.modules.auth.service;

import com.isxcode.spark.api.auth.constants.LoginLogStatus;
import com.isxcode.spark.api.auth.req.PageLoginLogReq;
import com.isxcode.spark.api.auth.res.PageLoginLogRes;
import com.isxcode.spark.common.security.ContextHolder;
import com.isxcode.spark.modules.auth.entity.LoginLogEntity;
import com.isxcode.spark.modules.auth.repository.LoginLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class LoginLogService {

    private static final String SYSTEM_USER = "system";

    private static final int TEXT_LIMIT = 1000;

    private final LoginLogRepository loginLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordSuccess(String loginMethod, String accountIdentifier, String userId, boolean registered) {

        record(loginMethod, accountIdentifier, userId, LoginLogStatus.SUCCESS, registered, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordFail(String loginMethod, String accountIdentifier, String userId, boolean registered,
        String errorMessage) {

        record(loginMethod, accountIdentifier, userId, LoginLogStatus.FAIL, registered, errorMessage);
    }

    public Page<PageLoginLogRes> pageLog(PageLoginLogReq pageLoginLogReq) {

        String searchKeyWord = pageLoginLogReq.getSearchKeyWord() == null ? "" : pageLoginLogReq.getSearchKeyWord();
        String loginMethod = pageLoginLogReq.getLoginMethod() == null ? "" : pageLoginLogReq.getLoginMethod();
        String loginStatus = pageLoginLogReq.getLoginStatus() == null ? "" : pageLoginLogReq.getLoginStatus();
        return loginLogRepository.pageLog(searchKeyWord, loginMethod, loginStatus,
            PageRequest.of(pageLoginLogReq.getPage(), pageLoginLogReq.getPageSize())).map(this::toPageLoginLogRes);
    }

    private void record(String loginMethod, String accountIdentifier, String userId, String loginStatus,
        boolean registered, String errorMessage) {

        withAuditUser(userId, () -> {
            LoginLogEntity loginLog = new LoginLogEntity();
            loginLog.setLoginMethod(loginMethod);
            loginLog.setAccountIdentifier(truncate(valueOrEmpty(accountIdentifier).trim()));
            loginLog.setUserId(userId);
            loginLog.setLoginStatus(loginStatus);
            loginLog.setRegistered(registered);
            loginLog.setErrorMessage(truncate(errorMessage));
            fillRequestInfo(loginLog);
            loginLogRepository.save(loginLog);
            return null;
        });
    }

    private void fillRequestInfo(LoginLogEntity loginLog) {

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            loginLog.setIpAddress("");
            loginLog.setUserAgent("");
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        loginLog.setIpAddress(truncate(resolveIpAddress(request)));
        loginLog.setUserAgent(truncate(valueOrEmpty(request.getHeader("User-Agent"))));
    }

    private String resolveIpAddress(HttpServletRequest request) {

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (!Strings.isEmpty(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (!Strings.isEmpty(realIp)) {
            return realIp;
        }
        return valueOrEmpty(request.getRemoteAddr());
    }

    private PageLoginLogRes toPageLoginLogRes(LoginLogEntity loginLog) {

        return PageLoginLogRes.builder().id(loginLog.getId()).loginMethod(loginLog.getLoginMethod())
            .accountIdentifier(loginLog.getAccountIdentifier()).userId(loginLog.getUserId())
            .ipAddress(loginLog.getIpAddress()).userAgent(loginLog.getUserAgent())
            .loginStatus(loginLog.getLoginStatus()).registered(loginLog.getRegistered())
            .errorMessage(loginLog.getErrorMessage()).createDateTime(loginLog.getCreateDateTime()).build();
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

    private <T> T withAuditUser(String userId, Supplier<T> supplier) {

        boolean needsContext = Strings.isEmpty(ContextHolder.getUserId());
        if (needsContext) {
            ContextHolder.setCurrentUser(Strings.isEmpty(userId) ? SYSTEM_USER : userId, null);
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
