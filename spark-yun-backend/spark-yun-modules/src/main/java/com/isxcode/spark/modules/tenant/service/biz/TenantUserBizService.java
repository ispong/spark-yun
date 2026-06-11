package com.isxcode.spark.modules.tenant.service.biz;

import com.isxcode.spark.common.security.ContextHolder;

import cn.hutool.core.util.DesensitizedUtil;
import com.isxcode.spark.api.tenant.req.*;
import com.isxcode.spark.api.tenant.res.PageTenantUserRes;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.api.user.constants.UserStatus;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.modules.license.repository.LicenseStore;
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
            throw new IsxAppException("该成员已经是项目成员");
        }

        // 初始化租户用户
        boolean normalAdmin = Boolean.TRUE.equals(turAddTenantUserReq.getIsTenantAdmin());
        TenantUserEntity tenantUserEntity = TenantUserEntity.builder().tenantId(tenantId)
            .userId(turAddTenantUserReq.getUserId()).status(UserStatus.ENABLE).normalAdmin(normalAdmin)
            .roleCode(normalAdmin ? RoleType.TENANT_NORMAL_ADMIN : RoleType.TENANT_MEMBER).build();

        // 判断用户当前是否有租户
        if (Strings.isEmpty(userEntity.getCurrentTenantId())) {
            userEntity.setCurrentTenantId(tenantId);
            userRepository.save(userEntity);
        }

        // 持久化数据
        tenantUserRepository.save(tenantUserEntity);
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
        tenantUserEntity.setRoleCode(RoleType.TENANT_NORMAL_ADMIN);

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
        member.setStatus(request.getStatus());
        tenantUserRepository.save(member);
    }

    public void setMemberRoles(SetMemberRolesReq request) {

        String tenantId = resolveTenantId(null);
        tenantUserRepository.findByTenantIdAndUserId(tenantId, request.getUserId())
            .orElseThrow(() -> new IsxAppException("成员不存在"));
        memberRoleRepository.deleteAllByTenantIdAndUserId(tenantId, request.getUserId());
        if (request.getRoleIds() == null) {
            return;
        }
        request.getRoleIds().stream().distinct().forEach(roleId -> {
            roleRepository.findById(roleId).filter(role -> tenantId.equals(role.getTenantId()))
                .orElseThrow(() -> new IsxAppException("角色不属于当前租户"));
            MemberRoleEntity memberRole = new MemberRoleEntity();
            memberRole.setTenantId(tenantId);
            memberRole.setUserId(request.getUserId());
            memberRole.setRoleId(roleId);
            memberRoleRepository.save(memberRole);
        });
    }

    private String resolveTenantId(String tenantId) {

        if (isSysAdmin()) {
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

        if (!isSysAdmin()
            && (Strings.isEmpty(ContextHolder.getTenantId()) || !ContextHolder.getTenantId().equals(tenantId))) {
            throw new IsxAppException("无权操作其他租户");
        }
    }

    private boolean isSysAdmin() {

        return SecurityContextHolder.getContext().getAuthentication() != null
            && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> RoleType.SYS_ADMIN.equals(authority.getAuthority()));
    }

    private void checkTenantAdminTarget(TenantUserEntity member) {

        TenantEntity tenant = tenantService.getTenant(member.getTenantId());
        if (member.getUserId().equals(tenant.getAdminUserId())) {
            throw new IsxAppException("租户管理员只能在平台管理中替换");
        }
    }
}
