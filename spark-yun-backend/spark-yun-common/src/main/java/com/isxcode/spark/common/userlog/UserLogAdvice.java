package com.isxcode.spark.common.userlog;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.isxcode.spark.backend.api.base.exceptions.SuccessResponseException;
import com.isxcode.spark.backend.api.base.pojos.BaseResponse;
import com.isxcode.spark.common.security.ContextHolder;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Aspect
@Slf4j
@Component
@RequiredArgsConstructor
public class UserLogAdvice {

    private final ApplicationEventPublisher eventPublisher;

    private final UserLogSettingProvider userLogSettingProvider;

    @Around(value = "@annotation(userLog)")
    public Object around(ProceedingJoinPoint joinPoint, UserLog userLog) throws Throwable {

        if (!userLogSettingProvider.enabled()) {
            return joinPoint.proceed();
        }

        long startTimestamp = System.currentTimeMillis();
        UserActionEntity userAction = buildUserAction(joinPoint, userLog, startTimestamp);
        try {
            Object result = joinPoint.proceed();
            fillSuccess(userAction, result, userLog.recordResponse());
            return result;
        } catch (SuccessResponseException e) {
            fillSuccess(userAction, e.getBaseResponse(), userLog.recordResponse());
            throw e;
        } catch (Throwable e) {
            fillFail(userAction, e);
            throw e;
        } finally {
            userAction.setEndTimestamp(System.currentTimeMillis());
            userAction.setDuration(userAction.getEndTimestamp() - userAction.getStartTimestamp());
            eventPublisher.publishEvent(new UserLogEvent(this, userAction));
        }
    }

    private UserActionEntity buildUserAction(ProceedingJoinPoint joinPoint, UserLog userLog, long startTimestamp) {

        UserActionEntity userAction = new UserActionEntity();
        userAction.setStartTimestamp(startTimestamp);
        userAction.setUserId(Strings.isEmpty(ContextHolder.getUserId()) ? "anonymous" : ContextHolder.getUserId());
        userAction
            .setTenantId(Strings.isEmpty(ContextHolder.getTenantId()) ? "anonymous" : ContextHolder.getTenantId());

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();
        if (request != null) {
            userAction.setReqPath(request.getRequestURI());
            userAction.setReqMethod(request.getMethod());
            userAction.setReqHeader(headerJson(request));
            userAction.setIpAddress(resolveIpAddress(request));
            userAction.setUserAgent(valueOrEmpty(request.getHeader("User-Agent")));
        }

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        UserLogDefinition definition = UserLogDictionary.resolve(userLog.moduleCode(), userLog.actionCode(),
            userAction.getReqPath() == null ? method.getName() : userAction.getReqPath());
        userAction.setModuleCode(definition.getModuleCode());
        userAction.setModuleName(definition.getModuleName());
        userAction.setLogType(definition.getLogType());
        userAction.setActionCode(definition.getActionCode());
        userAction.setActionName(definition.getActionName());
        userAction.setApiName(definition.getApiName());

        if (userLog.recordRequest()) {
            userAction.setReqBody(requestJson(request, joinPoint.getArgs()));
        }
        return userAction;
    }

    private void fillSuccess(UserActionEntity userAction, Object result, boolean recordResponse) {

        userAction.setStatus(resolveStatus(result));
        if (recordResponse) {
            userAction.setResBody(toJson(sanitize(result)));
        }
    }

    private void fillFail(UserActionEntity userAction, Throwable e) {

        userAction.setStatus(UserLogStatus.FAIL);
        userAction.setExceptionMessage(e.getMessage());
    }

    private String resolveStatus(Object result) {

        if (result instanceof BaseResponse<?> response && !"200".equals(response.getCode())) {
            return UserLogStatus.FAIL;
        }
        return UserLogStatus.SUCCESS;
    }

    private String requestJson(HttpServletRequest request, Object[] args) {

        try {
            JSONObject jsonObject = new JSONObject();
            if (request != null) {
                jsonObject.put("queryString", request.getQueryString());
            }
            jsonObject.put("body", Arrays.stream(args).map(this::sanitize).toList());
            return jsonObject.toJSONString();
        } catch (Exception e) {
            log.warn("Serialize user log request failed", e);
            return "Serialize request failed: " + e.getMessage();
        }
    }

    private Object sanitize(Object value) {

        if (value == null) {
            return null;
        }
        if (value instanceof MultipartFile file) {
            JSONObject fileJson = new JSONObject();
            fileJson.put("filename", file.getOriginalFilename());
            fileJson.put("contentType", file.getContentType());
            fileJson.put("size", file.getSize());
            return fileJson;
        }
        if (value instanceof MultipartFile[] files) {
            return Arrays.stream(files).map(this::sanitize).toList();
        }
        if (value instanceof ServletRequest || value instanceof ServletResponse || value instanceof InputStream
            || value instanceof OutputStream) {
            return value.getClass().getSimpleName();
        }
        return value;
    }

    private String headerJson(HttpServletRequest request) {

        JSONObject jsonObject = new JSONObject();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            jsonObject.put(name, request.getHeader(name));
        }
        return jsonObject.toJSONString();
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

    private String toJson(Object value) {

        try {
            return JSON.toJSONString(value);
        } catch (Exception e) {
            log.warn("Serialize user log content failed", e);
            return String.valueOf(value);
        }
    }

    private String valueOrEmpty(String value) {

        return value == null ? "" : value;
    }
}
