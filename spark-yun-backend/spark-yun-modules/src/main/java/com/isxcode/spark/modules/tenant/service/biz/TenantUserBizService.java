package com.isxcode.spark.modules.tenant.service.biz;

import com.isxcode.spark.common.security.ContextHolder;

import cn.hutool.core.util.DesensitizedUtil;
import com.isxcode.spark.api.tenant.req.*;
import com.isxcode.spark.api.tenant.res.PageTenantUserRes;
import com.isxcode.spark.api.tenant.res.TenantInviteRes;
import com.isxcode.spark.api.tenant.constants.TenantStatus;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.api.user.constants.UserStatus;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.modules.license.repository.LicenseStore;
import com.isxcode.spark.modules.tenant.entity.TenantInviteEntity;
import com.isxcode.spark.modules.tenant.repository.TenantInviteRepository;
import com.isxcode.spark.security.authorization.MemberRoleEntity;
import com.isxcode.spark.security.authorization.MemberRoleRepository;
import com.isxcode.spark.security.authorization.OrgMemberRepository;
import com.isxcode.spark.security.authorization.RoleRepository;
import com.isxcode.spark.security.user.TenantEntity;
import com.isxcode.spark.modules.tenant.service.TenantService;
import com.isxcode.spark.security.user.TenantUserEntity;
import com.isxcode.spark.security.user.TenantUserRepository;
import com.isxcode.spark.security.user.UserEntity;
import com.isxcode.spark.security.user.UserRepository;

import cn.hutool.core.util.IdUtil;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class TenantUserBizService {

    private final UserRepository userRepository;

    private final TenantUserRepository tenantUserRepository;

    private final TenantService tenantService;

    private final LicenseStore licenseStore;

    private final MemberRoleRepository memberRoleRepository;

    private final OrgMemberRepository orgMemberRepository;

    private final RoleRepository roleRepository;

    private final TenantInviteRepository tenantInviteRepository;

    public void addTenantUser(AddTenantUserReq turAddTenantUserReq) {

        String tenantId = resolveTenantId(turAddTenantUserReq.getTenantId());

        // 判断是否到租户的人员上限
        TenantEntity tenant = tenantService.getTenant(tenantId);
        long memberCount = tenantUserRepository.countByTenantId(tenantId);
        if (memberCount + 1 > tenant.getMaxMemberNum()) {
            throw new IsxAppException("超出租户的最大成员限制");
        }

        // 判断是否超过许可证成员最大值
        if (licenseStore.getLicense() != null) {
            if (memberCount + 1 > licenseStore.getLicense().getMaxMemberNum()) {
                throw new IsxAppException("超出许可证租户的最大成员限制");
            }
        }

        // 判断对象用户是否合法
        Optional<UserEntity> userEntityOptional = userRepository.findById(turAddTenantUserReq.getUserId());
        if (!userEntityOptional.isPresent()) {
            throw new IsxAppException("用户不存在");
        }
        UserEntity userEntity = userEntityOptional.get();

        // 判断该用户是否已经是成员
        Optional<TenantUserEntity> tenantUserEntityOptional =
            tenantUserRepository.findByTenantIdAndUserId(tenantId, turAddTenantUserReq.getUserId());
        if (tenantUserEntityOptional.isPresent()) {
            throw new IsxAppException("该用户已是租户成员");
        }

        // 初始化租户用户
        boolean normalAdmin = Boolean.TRUE.equals(turAddTenantUserReq.getIsTenantAdmin());
        TenantUserEntity tenantUserEntity = TenantUserEntity.builder().tenantId(tenantId)
            .userId(turAddTenantUserReq.getUserId()).status(UserStatus.ENABLE).normalAdmin(normalAdmin)
            .roleCode(normalAdmin ? RoleType.TENANT_ADMIN : RoleType.TENANT_MEMBER).build();

        // 判断用户当前是否有租户
        if (Strings.isEmpty(userEntity.getCurrentTenantId())) {
            userEntity.setCurrentTenantId(tenantId);
            userRepository.save(userEntity);
        }

        // 持久化数据
        tenantUserRepository.save(tenantUserEntity);
    }

    public TenantInviteRes getTenantInvite(GetTenantInviteReq request) {

        String tenantId = resolveTenantId(request.getTenantId());
        TenantInviteEntity invite = tenantInviteRepository.findFirstByTenantIdOrderByCreateDateTimeDesc(tenantId)
            .orElseGet(() -> createDefaultInvite(tenantId));
        return toTenantInviteRes(invite);
    }

    public TenantInviteRes saveTenantInvite(SaveTenantInviteReq request) {

        String tenantId = resolveTenantId(request.getTenantId());
        TenantInviteEntity invite = tenantInviteRepository.findFirstByTenantIdOrderByCreateDateTimeDesc(tenantId)
            .orElseGet(() -> createDefaultInvite(tenantId));

        if (Boolean.TRUE.equals(request.getRegenerate())) {
            invite.setInviteCode(generateInviteCode());
        }

        Integer validDays = request.getValidDays() == null ? 7 : request.getValidDays();
        if (!List.of(0, 1, 7, 30).contains(validDays)) {
            throw new IsxAppException("邀请码有效期不合法");
        }
        invite.setValidDays(validDays);
        invite.setExpireDateTime(validDays <= 0 ? null : LocalDateTime.now().plusDays(validDays));
        invite.setRoleIds(joinRoleIds(tenantId, request.getRoleIds()));
        return toTenantInviteRes(tenantInviteRepository.save(invite));
    }

    public void applyTenantInvite(ApplyTenantInviteReq request) {

        UserEntity user =
            userRepository.findById(ContextHolder.getUserId()).orElseThrow(() -> new IsxAppException("用户不存在"));
        if (RoleType.PLATFORM_SUPER_ADMIN.equals(user.getRoleCode())) {
            throw new IsxAppException("平台超级管理员不能申请租户");
        }

        TenantInviteEntity invite = tenantInviteRepository.findByInviteCode(request.getInviteCode().trim())
            .orElseThrow(() -> new IsxAppException("邀请码无效"));
        if (invite.getExpireDateTime() != null && LocalDateTime.now().isAfter(invite.getExpireDateTime())) {
            throw new IsxAppException("邀请码已过期，请联系管理员重新获取");
        }

        TenantEntity tenant = tenantService.getTenant(invite.getTenantId());
        if (!TenantStatus.ENABLE.equals(tenant.getStatus())) {
            throw new IsxAppException("邀请码对应租户不可用");
        }

        Optional<TenantUserEntity> memberOptional =
            tenantUserRepository.findByTenantIdAndUserId(invite.getTenantId(), user.getId());
        if (memberOptional.isPresent()) {
            TenantUserEntity member = memberOptional.get();
            if (UserStatus.APPLYING.equals(member.getStatus())) {
                throw new IsxAppException("你已提交申请，请等待管理员审核");
            }
            throw new IsxAppException("你已是该租户成员，无需重复申请");
        }

        TenantUserEntity member = TenantUserEntity.builder().tenantId(invite.getTenantId()).userId(user.getId())
            .status(UserStatus.APPLYING).normalAdmin(false).roleCode(RoleType.TENANT_MEMBER)
            .applyRoleIds(invite.getRoleIds()).applyInviteCode(invite.getInviteCode()).build();
        tenantUserRepository.save(member);
    }

    public Page<PageTenantUserRes> pageTenantUser(PageTenantUserReq turAddTenantUserReq) {

        String tenantId = resolveTenantId(turAddTenantUserReq.getTenantId());

        Page<PageTenantUserRes> tenantUserPage =
            tenantUserRepository.searchTenantUser(tenantId, turAddTenantUserReq.getSearchKeyWord(),
                PageRequest.of(turAddTenantUserReq.getPage(), turAddTenantUserReq.getPageSize()));

        tenantUserPage.getContent().forEach(item -> {
            item.setPhone(
                Strings.isEmpty(item.getPhone()) ? item.getPhone() : DesensitizedUtil.mobilePhone(item.getPhone()));
            item.setEmail(Strings.isEmpty(item.getEmail()) ? item.getEmail() : DesensitizedUtil.email(item.getEmail()));
            item.setRoleIds(memberRoleRepository.findAllByTenantIdAndUserId(tenantId, item.getUserId()).stream()
                .map(MemberRoleEntity::getRoleId).toList());
        });

        return tenantUserPage;
    }

    public void removeTenantUser(RemoveTenantUserReq removeTenantUserReq) {

        // 查询用户是否在租户中
        Optional<TenantUserEntity> tenantUserEntityOptional =
            tenantUserRepository.findById(removeTenantUserReq.getTenantUserId());
        if (!tenantUserEntityOptional.isPresent()) {
            throw new IsxAppException("用户不存在");
        }
        checkTenantPermission(tenantUserEntityOptional.get().getTenantId());

        TenantUserEntity member = tenantUserEntityOptional.get();
        checkTenantAdminTarget(member);

        // 删除租户用户
        memberRoleRepository.deleteAllByTenantIdAndUserId(member.getTenantId(), member.getUserId());
        orgMemberRepository.deleteAllByTenantIdAndUserId(member.getTenantId(), member.getUserId());
        tenantUserRepository.delete(member);
    }

    public void setTenantAdmin(SetTenantAdminReq setTenantAdminReq) {

        // 查询用户是否在租户中
        Optional<TenantUserEntity> tenantUserEntityOptional =
            tenantUserRepository.findById(setTenantAdminReq.getTenantUserId());
        if (!tenantUserEntityOptional.isPresent()) {
            throw new IsxAppException("用户不存在");
        }
        checkTenantPermission(tenantUserEntityOptional.get().getTenantId());

        checkTenantAdminTarget(tenantUserEntityOptional.get());

        // 兼容旧接口名称：设置普通管理员
        TenantUserEntity tenantUserEntity = tenantUserEntityOptional.get();
        tenantUserEntity.setNormalAdmin(true);
        tenantUserEntity.setRoleCode(RoleType.TENANT_ADMIN);

        // 持久化
        tenantUserRepository.save(tenantUserEntity);
    }

    public void removeTenantAdmin(RemoveTenantAdminReq removeTenantAdminReq) {

        // 查询用户是否在租户中
        Optional<TenantUserEntity> tenantUserEntityOptional =
            tenantUserRepository.findById(removeTenantAdminReq.getTenantUserId());
        if (!tenantUserEntityOptional.isPresent()) {
            throw new IsxAppException("用户不存在");
        }
        checkTenantPermission(tenantUserEntityOptional.get().getTenantId());

        checkTenantAdminTarget(tenantUserEntityOptional.get());

        // 兼容旧接口名称：取消普通管理员
        TenantUserEntity tenantUserEntity = tenantUserEntityOptional.get();
        tenantUserEntity.setNormalAdmin(false);
        tenantUserEntity.setRoleCode(RoleType.TENANT_MEMBER);

        // 持久化
        tenantUserRepository.save(tenantUserEntity);
    }

    public void setTenantMemberStatus(SetTenantMemberStatusReq request) {

        TenantUserEntity member =
            tenantUserRepository.findById(request.getTenantUserId()).orElseThrow(() -> new IsxAppException("成员不存在"));
        checkTenantPermission(member.getTenantId());
        checkTenantAdminTarget(member);
        if (UserStatus.APPLYING.equals(member.getStatus())) {
            throw new IsxAppException("申请中的成员不能直接启用或禁用");
        }
        member.setStatus(request.getStatus());
        tenantUserRepository.save(member);
    }

    public void setMemberRoles(SetMemberRolesReq request) {

        String tenantId = resolveTenantId(request.getTenantId());
        tenantUserRepository.findByTenantIdAndUserId(tenantId, request.getUserId())
            .orElseThrow(() -> new IsxAppException("成员不存在"));
        saveMemberRoles(tenantId, request.getUserId(), request.getRoleIds());
    }

    public void approveTenantApply(ReviewTenantApplyReq request) {

        TenantUserEntity member =
            tenantUserRepository.findById(request.getTenantUserId()).orElseThrow(() -> new IsxAppException("申请不存在"));
        checkTenantPermission(member.getTenantId());
        if (!UserStatus.APPLYING.equals(member.getStatus())) {
            throw new IsxAppException("该成员不是申请中状态");
        }

        member.setStatus(UserStatus.ENABLE);
        tenantUserRepository.save(member);
        saveMemberRoles(member.getTenantId(), member.getUserId(), splitRoleIds(member.getApplyRoleIds()));
    }

    public void rejectTenantApply(ReviewTenantApplyReq request) {

        TenantUserEntity member =
            tenantUserRepository.findById(request.getTenantUserId()).orElseThrow(() -> new IsxAppException("申请不存在"));
        checkTenantPermission(member.getTenantId());
        if (!UserStatus.APPLYING.equals(member.getStatus())) {
            throw new IsxAppException("该成员不是申请中状态");
        }
        tenantUserRepository.delete(member);
    }

    private TenantInviteEntity createDefaultInvite(String tenantId) {

        TenantInviteEntity invite = new TenantInviteEntity();
        invite.setTenantId(tenantId);
        invite.setInviteCode(generateInviteCode());
        invite.setValidDays(7);
        invite.setExpireDateTime(LocalDateTime.now().plusDays(7));
        invite.setRoleIds("");
        return tenantInviteRepository.save(invite);
    }

    private TenantInviteRes toTenantInviteRes(TenantInviteEntity invite) {

        return TenantInviteRes.builder().tenantId(invite.getTenantId()).inviteCode(invite.getInviteCode())
            .validDays(invite.getValidDays()).expireDateTime(invite.getExpireDateTime())
            .roleIds(splitRoleIds(invite.getRoleIds())).build();
    }

    private String generateInviteCode() {

        String inviteCode;
        do {
            inviteCode = IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase();
        } while (tenantInviteRepository.findByInviteCode(inviteCode).isPresent());
        return inviteCode;
    }

    private String joinRoleIds(String tenantId, List<String> roleIds) {

        if (roleIds == null || roleIds.isEmpty()) {
            return "";
        }
        List<String> distinctRoleIds = roleIds.stream().filter(roleId -> !Strings.isEmpty(roleId)).distinct().toList();
        distinctRoleIds.forEach(roleId -> roleRepository.findById(roleId)
            .filter(role -> tenantId.equals(role.getTenantId())).orElseThrow(() -> new IsxAppException("角色不属于当前租户")));
        return String.join(",", distinctRoleIds);
    }

    private List<String> splitRoleIds(String roleIds) {

        if (Strings.isEmpty(roleIds)) {
            return List.of();
        }
        return Arrays.stream(roleIds.split(",")).filter(roleId -> !Strings.isEmpty(roleId)).distinct().toList();
    }

    private void saveMemberRoles(String tenantId, String userId, List<String> roleIds) {

        memberRoleRepository.deleteAllByTenantIdAndUserId(tenantId, userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        roleIds.forEach(roleId -> {
            roleRepository.findById(roleId).filter(role -> tenantId.equals(role.getTenantId()))
                .orElseThrow(() -> new IsxAppException("角色不属于当前租户"));
            MemberRoleEntity memberRole = new MemberRoleEntity();
            memberRole.setTenantId(tenantId);
            memberRole.setUserId(userId);
            memberRole.setRoleId(roleId);
            memberRoleRepository.save(memberRole);
        });
    }

    private String resolveTenantId(String tenantId) {

        if (hasPlatformAccess()) {
            if (Strings.isEmpty(tenantId)) {
                throw new IsxAppException("请指定租户id");
            }
            return tenantId;
        }

        if (Strings.isEmpty(ContextHolder.getTenantId())) {
            throw new IsxAppException("租户id丢失");
        }

        if (!Strings.isEmpty(tenantId) && !ContextHolder.getTenantId().equals(tenantId)) {
            throw new IsxAppException("无权操作其他租户");
        }

        return ContextHolder.getTenantId();
    }

    private void checkTenantPermission(String tenantId) {

        if (!hasPlatformAccess()
            && (Strings.isEmpty(ContextHolder.getTenantId()) || !ContextHolder.getTenantId().equals(tenantId))) {
            throw new IsxAppException("无权操作其他租户");
        }
    }

    private boolean hasPlatformAccess() {

        return SecurityContextHolder.getContext().getAuthentication() != null
            && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> RoleType.PLATFORM_SUPER_ADMIN.equals(authority.getAuthority())
                    || RoleType.PLATFORM_ADMIN.equals(authority.getAuthority()));
    }

    private void checkTenantAdminTarget(TenantUserEntity member) {

        TenantEntity tenant = tenantService.getTenant(member.getTenantId());
        if (member.getUserId().equals(tenant.getAdminUserId())) {
            throw new IsxAppException("租户管理员只能在平台管理中替换");
        }
    }
}
