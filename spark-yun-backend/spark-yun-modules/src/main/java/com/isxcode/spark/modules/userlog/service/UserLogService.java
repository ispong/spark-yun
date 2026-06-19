package com.isxcode.spark.modules.userlog.service;

import com.isxcode.spark.api.userlog.req.PageUserLogReq;
import com.isxcode.spark.api.userlog.res.PageUserLogRes;
import com.isxcode.spark.api.userlog.res.UserLogDefinitionRes;
import com.isxcode.spark.common.userlog.UserActionEntity;
import com.isxcode.spark.common.userlog.UserActionRepository;
import com.isxcode.spark.common.userlog.UserLogDefinition;
import com.isxcode.spark.common.userlog.UserLogDictionary;
import com.isxcode.spark.common.userlog.UserLogSettingProvider;
import com.isxcode.spark.security.user.TenantEntity;
import com.isxcode.spark.security.user.TenantRepository;
import com.isxcode.spark.security.user.UserEntity;
import com.isxcode.spark.security.user.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class UserLogService {

    private final UserActionRepository userActionRepository;

    private final UserRepository userRepository;

    private final TenantRepository tenantRepository;

    private final UserLogSettingProvider userLogSettingProvider;

    public Page<PageUserLogRes> pageLog(PageUserLogReq request) {

        String searchKeyWord = request.getSearchKeyWord() == null ? "" : request.getSearchKeyWord();
        return userActionRepository
            .pageLog(searchKeyWord, request.getModuleCode(), request.getLogType(), request.getAccount(),
                request.getTenantId(), request.getStartDateTime(), request.getEndDateTime(),
                PageRequest.of(request.getPage(), request.getPageSize()))
            .map(this::toPageUserLogRes);
    }

    public List<UserLogDefinitionRes> definitions() {

        return UserLogDictionary.definitions().stream().map(this::toUserLogDefinitionRes).toList();
    }

    public void cleanExpiredLogs() {

        int retentionDays = userLogSettingProvider.retentionDays();
        userActionRepository.deleteExpired(LocalDateTime.now().minusDays(retentionDays));
    }

    private PageUserLogRes toPageUserLogRes(UserActionEntity userAction) {

        return PageUserLogRes.builder().id(userAction.getId()).userId(userAction.getUserId())
            .account(resolveAccount(userAction.getUserId())).tenantId(userAction.getTenantId())
            .tenantName(resolveTenantName(userAction.getTenantId())).logType(userAction.getLogType())
            .moduleCode(userAction.getModuleCode()).moduleName(userAction.getModuleName()).apiName(userAction.getApiName()).reqPath(userAction.getReqPath())
            .reqMethod(userAction.getReqMethod()).reqBody(userAction.getReqBody()).resBody(userAction.getResBody())
            .status(userAction.getStatus()).duration(userAction.getDuration()).ipAddress(userAction.getIpAddress())
            .userAgent(userAction.getUserAgent()).exceptionMessage(userAction.getExceptionMessage())
            .createDateTime(userAction.getCreateDateTime()).build();
    }

    private UserLogDefinitionRes toUserLogDefinitionRes(UserLogDefinition definition) {

        return UserLogDefinitionRes.builder().moduleCode(definition.getModuleCode())
            .moduleName(definition.getModuleName()).logType(definition.getLogType())
            .apiName(definition.getApiName()).build();
    }

    private String resolveAccount(String userId) {

        return userRepository.findById(userId).map(UserEntity::getAccount).orElse(userId);
    }

    private String resolveTenantName(String tenantId) {

        return tenantRepository.findById(tenantId).map(TenantEntity::getName).orElse(tenantId);
    }
}
