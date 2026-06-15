package com.isxcode.spark.modules.user.service;

import com.isxcode.spark.common.security.ContextHolder;
import com.isxcode.spark.common.security.CurrentUser;
import com.isxcode.spark.common.security.RefreshUserToken;


import cn.hutool.crypto.SecureUtil;
import com.isxcode.spark.api.tenant.constants.TenantStatus;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.api.user.constants.UserStatus;
import com.isxcode.spark.api.user.req.*;
import com.isxcode.spark.api.user.res.*;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.backend.api.base.properties.IsxAppProperties;
import com.isxcode.spark.common.utils.jwt.JwtUtils;
import com.isxcode.spark.modules.tenant.service.TenantService;
import com.isxcode.spark.security.authorization.AccessSnapshot;
import com.isxcode.spark.security.authorization.ProductAccessService;
import com.isxcode.spark.security.user.*;
import com.isxcode.spark.modules.user.mapper.UserMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class UserBizService {

    private static final String REFRESH_TOKEN = "REFRESH_TOKEN";

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final IsxAppProperties isxAppProperties;

    private final TenantRepository tenantRepository;

    private final TenantUserRepository tenantUserRepository;

    private final TenantService tenantService;

    private final ProductAccessService productAccessService;

    public LoginRes login(LoginReq usrLoginReq) {

        // 判断用户是否存在
        Optional<UserEntity> userEntityOptional = userRepository.findByAccount(usrLoginReq.getAccount());
        if (!userEntityOptional.isPresent()) {
            throw new IsxAppException("账号或者密码不正确");
        }
        UserEntity userEntity = userEntityOptional.get();

        // 判断用户是否在有效期内
        if (userEntity.getValidStartDateTime() != null && userEntity.getValidEndDateTime() != null) {
            if (LocalDateTime.now().isBefore(userEntity.getValidStartDateTime())
                || LocalDateTime.now().isAfter(userEntity.getValidEndDateTime())) {
                throw new IsxAppException("用户账号不在有效期内，请联系管理员");
            }
        }

        // 判断用户是否禁用
        if (UserStatus.DISABLE.equals(userEntity.getStatus())) {
            throw new IsxAppException("账号已被禁用，请联系管理员");
        }

        // 如果是系统管理员，首次登录，插入配置的密码并保存
        if (RoleType.PLATFORM_SUPER_ADMIN.equals(userEntity.getRoleCode()) && Strings.isEmpty(userEntity.getPasswd())) {
            userEntity.setPasswd(SecureUtil.md5(isxAppProperties.getAdminPasswd()));
            userRepository.save(userEntity);
        }

        // 判断密码是否合法
        if (!SecureUtil.md5(usrLoginReq.getPasswd()).equals(userEntity.getPasswd())) {
            throw new IsxAppException("账号或者密码不正确");
        }

        // 如果是平台超级管理员直接返回
        if (RoleType.PLATFORM_SUPER_ADMIN.equals(userEntity.getRoleCode())) {
            return buildLoginRes(userEntity, null, resolvePlatformRole(userEntity));
        }

        // 如果用户不在任何一个租户报错
        List<TenantUserEntity> tenantUserEntities =
            tenantUserRepository.findAllByUserIdAndStatus(userEntity.getId(), UserStatus.ENABLE);
        if (tenantUserEntities.isEmpty()) {
            if (isPlatformRole(userEntity)) {
                return buildLoginRes(userEntity, null, resolvePlatformRole(userEntity));
            }
            throw new IsxAppException("当前账号暂无可访问租户，请联系管理员。");
        }

        // 如果用户没有任何启动租户报错
        List<String> tenantId =
            tenantUserEntities.stream().map(TenantUserEntity::getTenantId).collect(Collectors.toList());
        List<TenantEntity> enableTenants = tenantRepository.findAllByIdInAndStatus(tenantId, TenantStatus.ENABLE);
        enableTenants = enableTenants.stream().filter(e -> {
            if (e.getValidStartDateTime() == null || e.getValidEndDateTime() == null) {
                return true;
            }
            return LocalDateTime.now().isAfter(e.getValidStartDateTime())
                && LocalDateTime.now().isBefore(e.getValidEndDateTime());
        }).collect(Collectors.toList());
        if (enableTenants.isEmpty()) {
            if (isPlatformRole(userEntity)) {
                return buildLoginRes(userEntity, null, resolvePlatformRole(userEntity));
            }
            throw new IsxAppException("当前账号暂无可访问租户，请联系管理员。");
        }

        // 如果用户当前租户id启动则返回当前租户，没有则随机挑一个
        List<String> enableTenantIds = enableTenants.stream().map(TenantEntity::getId).collect(Collectors.toList());
        String currentTenantId;
        if (!Strings.isEmpty(userEntity.getCurrentTenantId())
            && enableTenantIds.contains(userEntity.getCurrentTenantId())) {
            currentTenantId = userEntity.getCurrentTenantId();
        } else {
            currentTenantId = enableTenants.get(0).getId();
        }

        userEntity.setCurrentTenantId(currentTenantId);
        userRepository.save(userEntity);

        // 返回用户在租户中的角色
        Optional<TenantUserEntity> tenantUserEntityOptional =
            tenantUserRepository.findByTenantIdAndUserId(currentTenantId, userEntity.getId());
        if (!tenantUserEntityOptional.isPresent()) {
            throw new IsxAppException("无可用租户，请联系管理员");
        }

        // 生成token并返回
        return buildLoginRes(userEntity, currentTenantId, tenantUserEntityOptional.get().getRoleCode());
    }

    public GetUserRes getUser() {

        // 判断用户是否存在
        Optional<UserEntity> userEntityOptional = userRepository.findById(ContextHolder.getUserId());
        if (!userEntityOptional.isPresent()) {
            throw new IsxAppException("账号或者密码不正确");
        }
        UserEntity userEntity = userEntityOptional.get();

        // 判断用户是否禁用
        if (UserStatus.DISABLE.equals(userEntity.getStatus())) {
            throw new IsxAppException("账号已被禁用，请联系管理员");
        }

        // 如果是平台角色直接返回
        if (isPlatformRole(userEntity) && Strings.isEmpty(ContextHolder.getTenantId())) {
            return buildGetUserRes(userEntity, null, resolvePlatformRole(userEntity));
        }

        List<TenantUserEntity> memberships =
            tenantUserRepository.findAllByUserIdAndStatus(userEntity.getId(), UserStatus.ENABLE);
        List<String> memberTenantIds = memberships.stream().map(TenantUserEntity::getTenantId).toList();
        List<TenantEntity> availableTenants =
            tenantRepository.findAllByIdInAndStatus(memberTenantIds, TenantStatus.ENABLE).stream()
                .filter(this::isTenantInValidTime).toList();
        if (availableTenants.isEmpty()) {
            throw new IsxAppException("当前账号暂无可访问租户，请联系管理员。");
        }
        List<String> availableTenantIds = availableTenants.stream().map(TenantEntity::getId).toList();
        String currentTenantId =
            availableTenantIds.contains(userEntity.getCurrentTenantId()) ? userEntity.getCurrentTenantId()
                : availableTenantIds.get(0);
        if (!currentTenantId.equals(userEntity.getCurrentTenantId())) {
            userEntity.setCurrentTenantId(currentTenantId);
            userRepository.save(userEntity);
        }

        // 返回用户在租户中的角色
        Optional<TenantUserEntity> tenantUserEntityOptional =
            tenantUserRepository.findByTenantIdAndUserId(currentTenantId, userEntity.getId());
        if (!tenantUserEntityOptional.isPresent()) {
            throw new IsxAppException("无可用租户，请联系管理员");
        }

        // 生成token并返回
        return buildGetUserRes(userEntity, currentTenantId, tenantUserEntityOptional.get().getRoleCode());
    }

    public LoginRes refreshToken(RefreshTokenReq refreshTokenReq) {

        RefreshUserToken refreshUserToken;
        try {
            refreshUserToken = JwtUtils.decrypt(isxAppProperties.getJwtKey(), refreshTokenReq.getRefreshToken(),
                isxAppProperties.getAesSlat(), RefreshUserToken.class);
        } catch (Exception e) {
            throw new IsxAppException("401", "刷新token异常，请重新登录");
        }

        if (!REFRESH_TOKEN.equals(refreshUserToken.tokenType())) {
            throw new IsxAppException("401", "刷新token异常，请重新登录");
        }

        UserEntity userEntity = userRepository.findById(refreshUserToken.userId())
            .orElseThrow(() -> new IsxAppException("401", "刷新token异常，请重新登录"));
        validateUserStatus(userEntity);

        if (isPlatformRole(userEntity) && Strings.isEmpty(refreshUserToken.tenantId())) {
            String tenantId = refreshUserToken.tenantId();
            return buildLoginRes(userEntity, tenantId, resolvePlatformRole(userEntity));
        }

        TenantUserEntity tenantUserEntity = validateTenantUser(userEntity.getId(), refreshUserToken.tenantId());
        return buildLoginRes(userEntity, refreshUserToken.tenantId(), tenantUserEntity.getRoleCode());
    }

    public LoginRes buildAuthenticatedLoginRes(UserEntity userEntity, String tenantId) {

        if (isPlatformRole(userEntity) && Strings.isEmpty(tenantId)) {
            return buildLoginRes(userEntity, null, resolvePlatformRole(userEntity));
        }
        TenantUserEntity tenantUser = validateTenantUser(userEntity.getId(), tenantId);
        return buildLoginRes(userEntity, tenantId, tenantUser.getRoleCode());
    }

    public void logout() {

        System.out.println("用户退出登录");
    }

    public void addUser(AddUserReq usrAddUserReq) {

        validateUniqueUserFields(usrAddUserReq.getUsername(), usrAddUserReq.getAccount(), usrAddUserReq.getPhone(),
            usrAddUserReq.getEmail(), "");

        // UsrAddUserReq To UserEntity
        UserEntity userEntity = userMapper.addUserReqToUserEntity(usrAddUserReq);
        userEntity.setStatus(UserStatus.ENABLE);
        userEntity.setRoleCode(RoleType.PLATFORM_MEMBER);
        userEntity.setPlatformAdmin(false);
        userEntity.setPasswd(SecureUtil.md5(userEntity.getPasswd()));

        // 特殊处理时间
        if (usrAddUserReq.getValidDateTime() != null && usrAddUserReq.getValidDateTime().size() == 2) {
            userEntity.setValidStartDateTime(usrAddUserReq.getValidDateTime().get(0));
            userEntity.setValidEndDateTime(usrAddUserReq.getValidDateTime().get(1));
        }

        // 数据持久化
        userRepository.save(userEntity);
    }

    public void updateUser(UpdateUserReq usrUpdateUserReq) {

        // 判断用户是否存在
        Optional<UserEntity> userEntityOptional = userRepository.findById(usrUpdateUserReq.getId());
        if (!userEntityOptional.isPresent()) {
            throw new IsxAppException("用户不存在");
        }
        validateUniqueUserFields(usrUpdateUserReq.getUsername(), usrUpdateUserReq.getAccount(),
            usrUpdateUserReq.getPhone(), usrUpdateUserReq.getEmail(), usrUpdateUserReq.getId());

        UserEntity userEntity = userMapper.updateUserReqToUserEntity(usrUpdateUserReq, userEntityOptional.get());

        // 特殊处理时间
        if (usrUpdateUserReq.getValidDateTime() != null && usrUpdateUserReq.getValidDateTime().size() == 2) {
            userEntity.setValidStartDateTime(usrUpdateUserReq.getValidDateTime().get(0));
            userEntity.setValidEndDateTime(usrUpdateUserReq.getValidDateTime().get(1));
        } else {
            userEntity.setValidStartDateTime(null);
            userEntity.setValidEndDateTime(null);
        }

        userRepository.save(userEntity);
    }

    public void updateUserPassword(UpdateUserPasswordReq updateUserPasswordReq) {

        Optional<UserEntity> userEntityOptional = userRepository.findById(updateUserPasswordReq.getUserId());
        if (!userEntityOptional.isPresent()) {
            throw new IsxAppException("用户不存在");
        }

        if (!updateUserPasswordReq.getNewPassword().equals(updateUserPasswordReq.getConfirmPassword())) {
            throw new IsxAppException("两次输入的新密码不一致");
        }

        UserEntity userEntity = userEntityOptional.get();
        userEntity.setPasswd(SecureUtil.md5(updateUserPasswordReq.getNewPassword()));
        userRepository.save(userEntity);
    }

    public void disableUser(DisableUserReq disableUserReq) {

        Optional<UserEntity> userEntityOptional = userRepository.findById(disableUserReq.getUserId());
        if (!userEntityOptional.isPresent()) {
            throw new IsxAppException("用户不存在");
        }

        UserEntity userEntity = userEntityOptional.get();
        checkBuiltInAdmin(userEntity);
        userEntity.setStatus(UserStatus.DISABLE);
        userRepository.save(userEntity);
    }

    public void enableUser(EnableUserReq enableUserReq) {

        Optional<UserEntity> userEntityOptional = userRepository.findById(enableUserReq.getUserId());
        if (!userEntityOptional.isPresent()) {
            throw new IsxAppException("用户不存在");
        }

        UserEntity userEntity = userEntityOptional.get();
        userEntity.setStatus(UserStatus.ENABLE);
        userRepository.save(userEntity);
    }

    public void deleteUser(DeleteUserReq deleteUserReq) {

        Optional<UserEntity> userEntityOptional = userRepository.findById(deleteUserReq.getUserId());
        if (!userEntityOptional.isPresent()) {
            throw new IsxAppException("用户不存在");
        }

        checkBuiltInAdmin(userEntityOptional.get());
        userRepository.deleteById(deleteUserReq.getUserId());

        // 保留历史成员关系、任务归属和审计记录。
    }

    public Page<PageUserRes> pageUser(PageUserReq usrQueryAllUsersReq) {

        Page<UserEntity> userEntitiesPage = userRepository.searchAllUser(usrQueryAllUsersReq.getSearchKeyWord(),
            PageRequest.of(usrQueryAllUsersReq.getPage(), usrQueryAllUsersReq.getPageSize()));

        return userEntitiesPage.map(userMapper::userEntityToUsrQueryAllUsersRes);
    }

    public void setPlatformAdmin(SetPlatformAdminReq setPlatformAdminReq) {

        UserEntity target =
            userRepository.findById(setPlatformAdminReq.getUserId()).orElseThrow(() -> new IsxAppException("用户不存在"));
        checkBuiltInAdmin(target);
        boolean callerIsPlatformSuperAdmin = userRepository.findById(ContextHolder.getUserId())
            .map(user -> RoleType.PLATFORM_SUPER_ADMIN.equals(user.getRoleCode())).orElse(false);
        if (!callerIsPlatformSuperAdmin) {
            throw new IsxAppException("只有平台超级管理员可以设置平台管理员");
        }
        target.setPlatformAdmin(Boolean.TRUE.equals(setPlatformAdminReq.getPlatformAdmin()));
        target.setRoleCode(target.getPlatformAdmin() ? RoleType.PLATFORM_ADMIN : RoleType.PLATFORM_MEMBER);
        userRepository.save(target);
    }

    public Page<PageEnableUserRes> pageEnableUser(PageEnableUserReq usrQueryAllEnableUsersReq) {

        Page<UserEntity> userEntitiesPage =
            userRepository.searchAllEnableUser(usrQueryAllEnableUsersReq.getSearchKeyWord(),
                PageRequest.of(usrQueryAllEnableUsersReq.getPage(), usrQueryAllEnableUsersReq.getPageSize()));

        return userMapper.userEntityToUsrQueryAllEnableUsersResPage(userEntitiesPage);
    }

    public void updateUserInfo(UpdateUserInfoReq updateUserInfoReq) {

        // 获取当前信息
        Optional<UserEntity> userEntityOptional = userRepository.findById(ContextHolder.getUserId());
        if (!userEntityOptional.isPresent()) {
            throw new IsxAppException("用户不存在");
        }

        validateUniqueUserFields(userEntityOptional.get().getUsername(), userEntityOptional.get().getAccount(),
            updateUserInfoReq.getPhone(), updateUserInfoReq.getEmail(), userEntityOptional.get().getId());

        // 更新信息
        UserEntity userEntity = userMapper.updateUserInfoToUserEntity(updateUserInfoReq, userEntityOptional.get());

        userRepository.save(userEntity);
    }

    public void updateMyPassword(UpdateMyPasswordReq updateMyPasswordReq) {

        // 获取当前用户
        Optional<UserEntity> userEntityOptional = userRepository.findById(ContextHolder.getUserId());
        if (!userEntityOptional.isPresent()) {
            throw new IsxAppException("用户不存在");
        }

        // 校验新密码
        if (!updateMyPasswordReq.getNewPassword().equals(updateMyPasswordReq.getConfirmPassword())) {
            throw new IsxAppException("两次输入的新密码不一致");
        }

        if (updateMyPasswordReq.getOldPassword().equals(updateMyPasswordReq.getNewPassword())) {
            throw new IsxAppException("新密码不能与原密码相同");
        }

        UserEntity userEntity = userEntityOptional.get();

        // 校验原密码
        if (!SecureUtil.md5(updateMyPasswordReq.getOldPassword()).equals(userEntity.getPasswd())) {
            throw new IsxAppException("原密码不正确");
        }

        userEntity.setPasswd(SecureUtil.md5(updateMyPasswordReq.getNewPassword()));
        userRepository.save(userEntity);
    }

    public GetAnonymousTokenRes getAnonymousToken(GetAnonymousTokenReq getAnonymousTokenReq) {

        String jwtToken = JwtUtils.encrypt(isxAppProperties.getAesSlat(), new CurrentUser("sy_anonymous", null),
            isxAppProperties.getJwtKey(), getAnonymousTokenReq.getValidDay() * 24 * 60);

        return GetAnonymousTokenRes.builder().token(jwtToken).build();
    }

    private String generateUserToken(String userId, String tenantId) {

        return JwtUtils.encrypt(isxAppProperties.getAesSlat(), new CurrentUser(userId, tenantId),
            isxAppProperties.getJwtKey(), isxAppProperties.getExpirationMin());
    }

    private void validateUniqueUserFields(String username, String account, String phone, String email,
        String excludedUserId) {

        if (userRepository.countIncludingDeletedByUsername(username, excludedUserId) > 0) {
            throw new IsxAppException("用户名已存在");
        }
        if (userRepository.countIncludingDeletedByAccount(account, excludedUserId) > 0) {
            throw new IsxAppException("账号已存在");
        }
        if (!Strings.isEmpty(phone) && userRepository.countIncludingDeletedByPhone(phone, excludedUserId) > 0) {
            throw new IsxAppException("手机号已存在");
        }
        if (!Strings.isEmpty(email) && userRepository.countIncludingDeletedByEmail(email, excludedUserId) > 0) {
            throw new IsxAppException("邮箱已存在");
        }
    }

    private String generateRefreshToken(String userId, String tenantId) {

        return JwtUtils.encrypt(isxAppProperties.getAesSlat(), new RefreshUserToken(userId, tenantId, REFRESH_TOKEN),
            isxAppProperties.getJwtKey(), isxAppProperties.getRefreshExpirationMin());
    }

    private LoginRes buildLoginRes(UserEntity userEntity, String tenantId, String role) {

        AccessSnapshot access = productAccessService.resolve(userEntity.getId(), tenantId);
        return LoginRes.builder().username(userEntity.getUsername()).account(userEntity.getAccount())
            .phone(userEntity.getPhone()).email(userEntity.getEmail()).remark(userEntity.getRemark())
            .token(generateUserToken(userEntity.getId(), tenantId))
            .refreshToken(generateRefreshToken(userEntity.getId(), tenantId)).tenantId(tenantId)
            .role(resolveCompatibilityRole(access, role)).platformSuperAdmin(access.systemAdmin())
            .platformAdmin(access.platformAdmin()).platformMember(isPlatformMember(access, role))
            .tenantSuperAdmin(access.tenantAdmin())
            .tenantAdmin(access.normalAdmin()).tenantMember(access.hasTenantAccess())
            .workspaceAllPermissions(access.hasAllWorkspacePermissions()).permissions(List.copyOf(access.permissions()))
            .defaultArea(access.systemAdmin() || isPlatformMember(access, role) ? "platform" : "workspace").build();
    }

    private GetUserRes buildGetUserRes(UserEntity userEntity, String tenantId, String role) {

        AccessSnapshot access = productAccessService.resolve(userEntity.getId(), tenantId);
        return GetUserRes.builder().username(userEntity.getUsername()).account(userEntity.getAccount())
            .phone(userEntity.getPhone()).email(userEntity.getEmail()).remark(userEntity.getRemark())
            .token(generateUserToken(userEntity.getId(), tenantId))
            .refreshToken(generateRefreshToken(userEntity.getId(), tenantId)).tenantId(tenantId)
            .role(resolveCompatibilityRole(access, role)).systemAdmin(access.systemAdmin())
            .platformSuperAdmin(access.systemAdmin()).platformAdmin(access.platformAdmin())
            .platformMember(isPlatformMember(access, role)).tenantSuperAdmin(access.tenantAdmin())
            .tenantAdmin(access.normalAdmin())
            .tenantMember(access.hasTenantAccess()).normalAdmin(access.normalAdmin())
            .workspaceAllPermissions(access.hasAllWorkspacePermissions()).permissions(List.copyOf(access.permissions()))
            .defaultArea(access.systemAdmin() || isPlatformMember(access, role) ? "platform" : "workspace").build();
    }

    private String resolveCompatibilityRole(AccessSnapshot access, String fallbackRole) {

        if (access.systemAdmin()) {
            return RoleType.PLATFORM_SUPER_ADMIN;
        }
        if (access.tenantAdmin()) {
            return RoleType.TENANT_SUPER_ADMIN;
        }
        if (access.normalAdmin()) {
            return RoleType.TENANT_ADMIN;
        }
        if (RoleType.TENANT_MEMBER.equals(fallbackRole)) {
            return RoleType.TENANT_MEMBER;
        }
        if (access.platformAdmin()) {
            return RoleType.PLATFORM_ADMIN;
        }
        if (RoleType.PLATFORM_MEMBER.equals(fallbackRole)) {
            return RoleType.PLATFORM_MEMBER;
        }
        return fallbackRole == null ? RoleType.TENANT_MEMBER : fallbackRole;
    }

    private void checkBuiltInAdmin(UserEntity userEntity) {

        if (RoleType.PLATFORM_SUPER_ADMIN.equals(userEntity.getRoleCode())) {
            throw new IsxAppException("超级管理员账号不允许执行该操作");
        }
    }

    private boolean isPlatformRole(UserEntity userEntity) {

        return isPlatformManagementRole(userEntity) || RoleType.PLATFORM_MEMBER.equals(userEntity.getRoleCode());
    }

    private boolean isPlatformManagementRole(UserEntity userEntity) {

        return RoleType.PLATFORM_SUPER_ADMIN.equals(userEntity.getRoleCode())
            || RoleType.PLATFORM_ADMIN.equals(userEntity.getRoleCode())
            || Boolean.TRUE.equals(userEntity.getPlatformAdmin());
    }

    private String resolvePlatformRole(UserEntity userEntity) {

        if (RoleType.PLATFORM_SUPER_ADMIN.equals(userEntity.getRoleCode())) {
            return RoleType.PLATFORM_SUPER_ADMIN;
        }
        if (RoleType.PLATFORM_ADMIN.equals(userEntity.getRoleCode())
            || Boolean.TRUE.equals(userEntity.getPlatformAdmin())) {
            return RoleType.PLATFORM_ADMIN;
        }
        return RoleType.PLATFORM_MEMBER;
    }

    private boolean isPlatformMember(AccessSnapshot access, String role) {

        return !access.systemAdmin() && !access.platformAdmin() && RoleType.PLATFORM_MEMBER.equals(role);
    }

    private boolean isTenantInValidTime(TenantEntity tenant) {

        return tenant.getValidStartDateTime() == null || tenant.getValidEndDateTime() == null
            || (!LocalDateTime.now().isBefore(tenant.getValidStartDateTime())
                && !LocalDateTime.now().isAfter(tenant.getValidEndDateTime()));
    }

    private void validateUserStatus(UserEntity userEntity) {

        if (UserStatus.DISABLE.equals(userEntity.getStatus())) {
            throw new IsxAppException("401", "账号已被禁用，请联系管理员");
        }

        if (userEntity.getValidStartDateTime() != null && userEntity.getValidEndDateTime() != null) {
            if (LocalDateTime.now().isBefore(userEntity.getValidStartDateTime())
                || LocalDateTime.now().isAfter(userEntity.getValidEndDateTime())) {
                throw new IsxAppException("401", "用户账号不在有效期内，请联系管理员");
            }
        }
    }

    private TenantUserEntity validateTenantUser(String userId, String tenantId) {

        if (Strings.isEmpty(tenantId)) {
            throw new IsxAppException("401", "刷新token异常，请重新登录");
        }

        TenantUserEntity tenantUserEntity = tenantUserRepository.findByTenantIdAndUserId(tenantId, userId)
            .orElseThrow(() -> new IsxAppException("401", "用户不在租户中，请重新登录"));

        if (TenantStatus.DISABLE.equals(tenantUserEntity.getStatus())) {
            throw new IsxAppException("401", "用户被租户禁用，请重新登录");
        }

        TenantEntity tenantEntity =
            tenantRepository.findById(tenantId).orElseThrow(() -> new IsxAppException("401", "当前租户不可用"));
        if (TenantStatus.DISABLE.equals(tenantEntity.getStatus())) {
            throw new IsxAppException("401", "当前租户已被禁用");
        }

        if (tenantEntity.getValidStartDateTime() != null && tenantEntity.getValidEndDateTime() != null) {
            if (LocalDateTime.now().isBefore(tenantEntity.getValidStartDateTime())
                || LocalDateTime.now().isAfter(tenantEntity.getValidEndDateTime())) {
                throw new IsxAppException("401", "当前租户不在有效期内");
            }
        }

        return tenantUserEntity;
    }
}
