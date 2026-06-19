package com.isxcode.spark.modules.tenant.service.biz;

import static com.isxcode.spark.common.jpa.JpaTenantContext.allData;

import com.isxcode.spark.common.security.ContextHolder;
import com.isxcode.spark.common.security.CurrentUser;


import com.isxcode.spark.api.tenant.constants.TenantStatus;
import com.isxcode.spark.api.tenant.req.*;
import com.isxcode.spark.api.tenant.res.ChooseTenantRes;
import com.isxcode.spark.api.tenant.res.GetTenantRes;
import com.isxcode.spark.api.tenant.res.PageTenantRes;
import com.isxcode.spark.api.tenant.res.QueryUserTenantRes;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.api.user.req.AddUserReq;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.backend.api.base.properties.IsxAppProperties;
import com.isxcode.spark.common.security.RefreshUserToken;
import com.isxcode.spark.common.utils.jwt.JwtUtils;
import com.isxcode.spark.modules.license.repository.LicenseStore;
import com.isxcode.spark.modules.user.service.UserService;
import com.isxcode.spark.security.authorization.AccessSnapshot;
import com.isxcode.spark.security.authorization.MemberRoleRepository;
import com.isxcode.spark.security.authorization.OrgMemberRepository;
import com.isxcode.spark.security.authorization.ProductAccessService;
import com.isxcode.spark.security.user.*;
import com.isxcode.spark.modules.tenant.mapper.TenantMapper;
import com.isxcode.spark.modules.user.service.UserBizService;
import com.isxcode.spark.modules.workflow.repository.WorkflowRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class TenantBizService {

    private static final String REFRESH_TOKEN = "REFRESH_TOKEN";

    private final TenantRepository tenantRepository;

    private final UserRepository userRepository;

    private final TenantUserRepository tenantUserRepository;

    private final TenantMapper tenantMapper;

    private final WorkflowRepository workflowRepository;

    private final LicenseStore licenseStore;

    private final IsxAppProperties isxAppProperties;

    private final ProductAccessService productAccessService;

    private final MemberRoleRepository memberRoleRepository;

    private final OrgMemberRepository orgMemberRepository;

    private final UserBizService userBizService;
    private final UserService userService;

    public void addTenant(AddTenantReq tetAddTenantReq) {

        if (Boolean.TRUE.equals(tetAddTenantReq.getCreateAdminUser())) {
            if (Strings.isEmpty(tetAddTenantReq.getAdminUsername())
                || Strings.isEmpty(tetAddTenantReq.getAdminAccount())
                || Strings.isEmpty(tetAddTenantReq.getAdminPassword())) {
                throw new IsxAppException("新租户管理员的用户名、账号和密码不能为空");
            }
            AddUserReq addUserReq = new AddUserReq();
            addUserReq.setUsername(tetAddTenantReq.getAdminUsername());
            addUserReq.setAccount(tetAddTenantReq.getAdminAccount());
            addUserReq.setPasswd(tetAddTenantReq.getAdminPassword());
            addUserReq.setPhone(tetAddTenantReq.getAdminPhone());
            addUserReq.setEmail(tetAddTenantReq.getAdminEmail());
            userBizService.addUser(addUserReq);
            tetAddTenantReq.setAdminUserId(userRepository.findByAccount(tetAddTenantReq.getAdminAccount())
                .orElseThrow(() -> new IsxAppException("租户管理员创建失败")).getId());
        }
        if (Strings.isEmpty(tetAddTenantReq.getAdminUserId())) {
            throw new IsxAppException("租户管理员不能为空");
        }

        if (tetAddTenantReq.getMaxWorkflowNum() == null) {
            tetAddTenantReq.setMaxWorkflowNum(1);
        }
        if (tetAddTenantReq.getMaxMemberNum() == null) {
            tetAddTenantReq.setMaxMemberNum(1);
        }

        // 判断租户数量是否达到上限
        if (licenseStore.getLicense() != null) {
            // 获取租户数总和
            long tenantCount = tenantRepository.count();
            // 比较租户数最大值
            if (licenseStore.getLicense().getMaxTenantNum() < tenantCount + 1) {
                throw new IsxAppException("租户数超出许可证限制数:" + licenseStore.getLicense().getMaxTenantNum() + ",请升级许可证");
            }
            // 比较成员数最大值
            if (licenseStore.getLicense().getMaxMemberNum() < tetAddTenantReq.getMaxMemberNum()) {
                throw new IsxAppException("成员数超出许可证限制数:" + licenseStore.getLicense().getMaxMemberNum() + ",请升级许可证");
            }
            // 比较作业流数最大值
            if (licenseStore.getLicense().getMaxWorkflowNum() < tetAddTenantReq.getMaxWorkflowNum()) {
                throw new IsxAppException("作业流数超出许可证限制数:" + licenseStore.getLicense().getMaxWorkflowNum() + ",请升级许可证");
            }
        }

        // 判断名称是否存在
        Optional<TenantEntity> tenantEntityOptional = tenantRepository.findByName(tetAddTenantReq.getName());
        if (tenantEntityOptional.isPresent()) {
            throw new IsxAppException("租户名称重复");
        }

        // 判断管理员是否存在
        UserEntity userEntity =
            userRepository.findById(tetAddTenantReq.getAdminUserId()).orElseThrow(() -> new IsxAppException("用户不存在"));

        TenantEntity tenant = tenantMapper.tetAddTenantReqToTenantEntity(tetAddTenantReq);
        if (tetAddTenantReq.getMaxMemberNum() != null) {
            tenant.setMaxMemberNum(Long.parseLong(String.valueOf(tetAddTenantReq.getMaxMemberNum())));
        }
        if (tetAddTenantReq.getMaxWorkflowNum() != null) {
            tenant.setMaxWorkflowNum(Long.parseLong(String.valueOf(tetAddTenantReq.getMaxWorkflowNum())));
        }

        // 有效期保存
        if (tetAddTenantReq.getValidDateTime() != null && tetAddTenantReq.getValidDateTime().size() == 2) {
            tenant.setValidStartDateTime(tetAddTenantReq.getValidDateTime().get(0));
            tenant.setValidEndDateTime(tetAddTenantReq.getValidDateTime().get(1));
        }

        // 持久化租户
        TenantEntity tenantEntity = tenantRepository.save(tenant);
        tenantEntity.setAdminUserId(tetAddTenantReq.getAdminUserId());
        tenantRepository.save(tenantEntity);

        // 初始化租户管理员
        TenantUserEntity tenantUserEntity =
            TenantUserEntity.builder().userId(tetAddTenantReq.getAdminUserId()).tenantId(tenantEntity.getId())
                .roleCode(RoleType.TENANT_SUPER_ADMIN).normalAdmin(false).status(TenantStatus.ENABLE).build();

        // 判断管理员是否绑定新租户
        if (Strings.isEmpty(userEntity.getCurrentTenantId())) {
            userEntity.setCurrentTenantId(tenantEntity.getId());
            userRepository.save(userEntity);
        }

        // 持久化租户管理员关系
        tenantUserRepository.save(tenantUserEntity);
    }

    public List<QueryUserTenantRes> queryUserTenant() {

        List<String> tenantIds;
        if ("admin_id".equals(ContextHolder.getUserId())) {
            return List.of();
        } else {
            List<TenantUserEntity> tenantUserEntities =
                tenantUserRepository.findAllByUserIdAndStatus(ContextHolder.getUserId(), TenantStatus.ENABLE);
            tenantIds = tenantUserEntities.stream().map(TenantUserEntity::getTenantId).collect(Collectors.toList());
            if (tenantUserEntities.isEmpty()) {
                throw new IsxAppException("当前账号暂无可访问租户，请联系管理员。");
            }
        }

        List<TenantEntity> tenantEntityList = tenantRepository.findAllByIdInAndStatus(tenantIds, TenantStatus.ENABLE)
            .stream().filter(this::isInValidTime).toList();
        tenantIds = tenantEntityList.stream().map(TenantEntity::getId).toList();
        if (tenantIds.isEmpty()) {
            throw new IsxAppException("当前账号暂无可访问租户，请联系管理员。");
        }

        // 查询用户最近一次租户
        UserEntity userEntity = userRepository.findById(ContextHolder.getUserId()).get();
        if (!tenantIds.isEmpty() && !tenantIds.contains(userEntity.getCurrentTenantId())) {
            userEntity.setCurrentTenantId(tenantIds.get(0));
            // 更新用户最近一次租户
            userRepository.save(userEntity);
        }

        // TenantEntity To TetQueryUserTenantRes
        List<QueryUserTenantRes> userTenantResList =
            tenantMapper.tenantEntityToTetQueryUserTenantResList(tenantEntityList);

        // 标记当前租户
        userTenantResList.forEach(e -> {
            if (e.getId().equals(userEntity.getCurrentTenantId())) {
                e.setCurrentTenant(true);
            }
        });

        return userTenantResList;
    }

    public Page<PageTenantRes> pageTenant(PageTenantReq tetQueryTenantReq) {

        Page<TenantEntity> tenantEntityPage = tenantRepository.searchAll(tetQueryTenantReq.getSearchKeyWord(),
            PageRequest.of(tetQueryTenantReq.getPage(), tetQueryTenantReq.getPageSize()));

        Page<PageTenantRes> result = tenantEntityPage.map(tenantMapper::tenantEntityToTetQueryTenantRes);
        result.getContent().forEach(e -> {
            e.setUsedWorkflowNum(String.valueOf(allData(() -> workflowRepository.countByTenantId(e.getId()))));
            e.setUsedMemberNum(String.valueOf(tenantUserRepository.countByTenantId(e.getId())));
            e.setAdminUserName(String.valueOf(userService.getUserName(e.getAdminUserId())));
        });
        return result;
    }

    public void updateTenantForSystemAdmin(UpdateTenantForSystemAdminReq tetUpdateTenantBySystemAdminReq) {

        // 判断租户是否存在
        Optional<TenantEntity> tenantEntityOptional =
            tenantRepository.findById(tetUpdateTenantBySystemAdminReq.getId());
        if (!tenantEntityOptional.isPresent()) {
            throw new IsxAppException("租户不存在");
        }

        // 判断租户数量是否达到上限
        if (licenseStore.getLicense() != null) {
            // 获取租户数总和
            long tenantCount = tenantRepository.count();
            // 比较租户数最大值
            if (licenseStore.getLicense().getMaxTenantNum() < tenantCount + 1) {
                throw new IsxAppException("租户数超出许可证限制数:" + licenseStore.getLicense().getMaxTenantNum() + ",请升级许可证");
            }
            // 比较成员数最大值
            if (licenseStore.getLicense().getMaxMemberNum() < tetUpdateTenantBySystemAdminReq.getMaxMemberNum()) {
                throw new IsxAppException("成员数超出许可证限制数:" + licenseStore.getLicense().getMaxMemberNum() + ",请升级许可证");
            }
            // 比较作业流数最大值
            if (licenseStore.getLicense().getMaxWorkflowNum() < tetUpdateTenantBySystemAdminReq.getMaxWorkflowNum()) {
                throw new IsxAppException("作业流数超出许可证限制数:" + licenseStore.getLicense().getMaxWorkflowNum() + ",请升级许可证");
            }
        }

        // TetUpdateTenantBySystemAdminReq To TenantEntity
        TenantEntity tenantEntity = tenantMapper
            .tetUpdateTenantBySystemAdminReqToTenantEntity(tetUpdateTenantBySystemAdminReq, tenantEntityOptional.get());

        // 有效期保存
        if (tetUpdateTenantBySystemAdminReq.getValidDateTime() != null
            && tetUpdateTenantBySystemAdminReq.getValidDateTime().size() == 2) {
            tenantEntity.setValidStartDateTime(tetUpdateTenantBySystemAdminReq.getValidDateTime().get(0));
            tenantEntity.setValidEndDateTime(tetUpdateTenantBySystemAdminReq.getValidDateTime().get(1));
        } else {
            tenantEntity.setValidStartDateTime(null);
            tenantEntity.setValidEndDateTime(null);
        }

        // 持久化对象
        tenantRepository.save(tenantEntity);
    }

    public void updateTenantForTenantAdmin(UpdateTenantForTenantAdminReq tetUpdateTenantByTenantAdminReq) {

        if (!tetUpdateTenantByTenantAdminReq.getId().equals(ContextHolder.getTenantId())) {
            throw new IsxAppException("无权操作其他租户");
        }

        // 判断租户是否存在
        Optional<TenantEntity> tenantEntityOptional =
            tenantRepository.findById(tetUpdateTenantByTenantAdminReq.getId());
        if (!tenantEntityOptional.isPresent()) {
            throw new IsxAppException("租户不存在");
        }

        tenantRepository.findByName(tetUpdateTenantByTenantAdminReq.getName()).ifPresent(tenantEntity -> {
            if (!tenantEntity.getId().equals(tetUpdateTenantByTenantAdminReq.getId())) {
                throw new IsxAppException("租户名称重复");
            }
        });

        // TetUpdateTenantByTenantAdminReq To TenantEntity
        TenantEntity tenantEntity = tenantMapper
            .tetUpdateTenantByTenantAdminReqToTenantEntity(tetUpdateTenantByTenantAdminReq, tenantEntityOptional.get());

        // 持久化对象
        tenantRepository.save(tenantEntity);
    }

    public void enableTenant(EnableTenantReq enableTenantReq) {

        // 判断租户是否存在
        Optional<TenantEntity> tenantEntityOptional = tenantRepository.findById(enableTenantReq.getTenantId());
        if (!tenantEntityOptional.isPresent()) {
            throw new IsxAppException("租户不存在");
        }

        // 设置为启用
        TenantEntity tenantEntity = tenantEntityOptional.get();
        tenantEntity.setStatus(TenantStatus.ENABLE);

        // 持久化
        tenantRepository.save(tenantEntity);
    }

    public void disableTenant(DisableTenantReq disableTenantReq) {

        // 判断租户是否存在
        Optional<TenantEntity> tenantEntityOptional = tenantRepository.findById(disableTenantReq.getTenantId());
        if (!tenantEntityOptional.isPresent()) {
            throw new IsxAppException("租户不存在");
        }

        // 设置为启用
        TenantEntity tenantEntity = tenantEntityOptional.get();
        tenantEntity.setStatus(TenantStatus.DISABLE);

        // 持久化
        tenantRepository.save(tenantEntity);
    }

    public void deleteTenant(DeleteTenantReq deleteTenantReq) {

        TenantEntity tenant =
            tenantRepository.findById(deleteTenantReq.getTenantId()).orElseThrow(() -> new IsxAppException("租户不存在"));
        if (!Strings.isEmpty(deleteTenantReq.getTenantName())
            && !tenant.getName().equals(deleteTenantReq.getTenantName())) {
            throw new IsxAppException("租户名称确认不一致");
        }
        tenantRepository.deleteById(deleteTenantReq.getTenantId());
    }

    public void replaceTenantAdmin(ReplaceTenantAdminReq request) {

        TenantEntity tenant =
            tenantRepository.findById(request.getTenantId()).orElseThrow(() -> new IsxAppException("租户不存在"));
        UserEntity newAdmin =
            userRepository.findById(request.getNewAdminUserId()).orElseThrow(() -> new IsxAppException("新租户管理员不存在"));
        if (RoleType.PLATFORM_SUPER_ADMIN.equals(newAdmin.getRoleCode())) {
            throw new IsxAppException("超级管理员不能成为租户管理员");
        }
        if (request.getNewAdminUserId().equals(tenant.getAdminUserId())) {
            return;
        }

        String oldAdminUserId = tenant.getAdminUserId();
        if (!Strings.isEmpty(oldAdminUserId)) {
            tenantUserRepository.findByTenantIdAndUserId(tenant.getId(), oldAdminUserId).ifPresent(oldMember -> {
                if ("REMOVE".equals(request.getOldAdminAction())) {
                    memberRoleRepository.deleteAllByTenantIdAndUserId(tenant.getId(), oldAdminUserId);
                    orgMemberRepository.deleteAllByTenantIdAndUserId(tenant.getId(), oldAdminUserId);
                    tenantUserRepository.delete(oldMember);
                } else {
                    oldMember.setRoleCode(RoleType.TENANT_MEMBER);
                    oldMember.setNormalAdmin(false);
                    tenantUserRepository.save(oldMember);
                }
            });
        }

        TenantUserEntity newMember =
            tenantUserRepository.findByTenantIdAndUserId(tenant.getId(), newAdmin.getId()).orElseGet(() -> {
                TenantUserEntity member = new TenantUserEntity();
                member.setTenantId(tenant.getId());
                member.setUserId(newAdmin.getId());
                return member;
            });
        newMember.setStatus(TenantStatus.ENABLE);
        newMember.setNormalAdmin(false);
        newMember.setRoleCode(RoleType.TENANT_SUPER_ADMIN);
        tenantUserRepository.save(newMember);
        tenant.setAdminUserId(newAdmin.getId());
        tenantRepository.save(tenant);
    }

    public void checkTenant(CheckTenantReq checkTenantReq) {

        // 判断租户是否存在
        Optional<TenantEntity> tenantEntityOptional = tenantRepository.findById(checkTenantReq.getTenantId());
        if (!tenantEntityOptional.isPresent()) {
            throw new IsxAppException("租户不存在");
        }
        TenantEntity tenantEntity = tenantEntityOptional.get();

        // 统计作业流数量
        long usedWorkflowNum = allData(() -> workflowRepository.countByTenantId(checkTenantReq.getTenantId()));
        tenantEntity.setUsedWorkflowNum(usedWorkflowNum);

        // 统计成员数量
        long memberNum = tenantUserRepository.countByTenantId(checkTenantReq.getTenantId());
        tenantEntity.setUsedMemberNum(memberNum);
        tenantEntity.setCheckDateTime(LocalDateTime.now());

        // 持久化
        tenantRepository.save(tenantEntity);
    }

    public ChooseTenantRes chooseTenant(ChooseTenantReq chooseTenantReq) {

        Optional<TenantEntity> tenantEntityOptional = tenantRepository.findById(chooseTenantReq.getTenantId());
        if (!tenantEntityOptional.isPresent()) {
            throw new IsxAppException("租户不存在");
        }

        // 判断租户是否在有效期内
        TenantEntity tenant = tenantEntityOptional.get();
        if (tenant.getValidStartDateTime() != null && tenant.getValidEndDateTime() != null) {
            if (LocalDateTime.now().isBefore(tenant.getValidStartDateTime())
                || LocalDateTime.now().isAfter(tenant.getValidEndDateTime())) {
                throw new IsxAppException("当前租户不在有效期内");
            }
        }

        Optional<UserEntity> userEntityOptional = userRepository.findById(ContextHolder.getUserId());
        if (!userEntityOptional.isPresent()) {
            throw new IsxAppException("用户不存在");
        }

        // 判断租户是否被禁用
        if (TenantStatus.DISABLE.equals(tenantEntityOptional.get().getStatus())) {
            throw new IsxAppException("该租户已被禁用，请联系管理员");
        }

        UserEntity userEntity = userEntityOptional.get();
        if (RoleType.PLATFORM_SUPER_ADMIN.equals(userEntity.getRoleCode())) {
            throw new IsxAppException("超级管理员不支持切换租户");
        }
        TenantUserEntity tenantUser =
            tenantUserRepository.findByTenantIdAndUserId(chooseTenantReq.getTenantId(), userEntity.getId())
                .orElseThrow(() -> new IsxAppException("用户不在租户中"));
        if (!TenantStatus.ENABLE.equals(tenantUser.getStatus())) {
            throw new IsxAppException("当前成员已被禁用");
        }
        userEntity.setCurrentTenantId(chooseTenantReq.getTenantId());
        userRepository.save(userEntity);

        AccessSnapshot access = productAccessService.resolve(userEntity.getId(), chooseTenantReq.getTenantId());
        String role = access.tenantAdmin() ? RoleType.TENANT_SUPER_ADMIN
            : access.normalAdmin() ? RoleType.TENANT_ADMIN : RoleType.TENANT_MEMBER;

        String token = JwtUtils.encrypt(isxAppProperties.getAesSlat(),
            new CurrentUser(userEntity.getId(), chooseTenantReq.getTenantId()), isxAppProperties.getJwtKey(),
            isxAppProperties.getExpirationMin());
        String refreshToken = JwtUtils.encrypt(isxAppProperties.getAesSlat(),
            new RefreshUserToken(userEntity.getId(), chooseTenantReq.getTenantId(), REFRESH_TOKEN),
            isxAppProperties.getJwtKey(), isxAppProperties.getRefreshExpirationMin());

        return ChooseTenantRes.builder().token(token).refreshToken(refreshToken).tenantId(chooseTenantReq.getTenantId())
            .role(role).systemAdmin(false).platformSuperAdmin(false).platformAdmin(access.platformAdmin())
            .tenantSuperAdmin(access.tenantAdmin()).tenantAdmin(access.normalAdmin())
            .tenantMember(access.hasTenantAccess()).normalAdmin(access.normalAdmin())
            .workspaceAllPermissions(access.hasAllWorkspacePermissions())
            .permissions(List.copyOf(access.frontendPermissionCodes()))
            .frontendPermissionCodes(List.copyOf(access.frontendPermissionCodes()))
            .defaultArea("workspace").build();
    }

    public GetTenantRes getTenant(GetTenantReq getTenantReq) {

        Optional<TenantEntity> tenantEntityOptional = tenantRepository.findById(getTenantReq.getTenantId());
        if (!tenantEntityOptional.isPresent()) {
            throw new IsxAppException("租户不存在");
        }
        TenantEntity tenantEntity = tenantEntityOptional.get();

        Optional<UserEntity> userEntityOptional = userRepository.findById(ContextHolder.getUserId());
        if (!userEntityOptional.isPresent()) {
            throw new IsxAppException("用户不存在");
        }
        UserEntity userEntity = userEntityOptional.get();

        // 如果是管理员直接返回
        if (RoleType.PLATFORM_SUPER_ADMIN.equals(userEntity.getRoleCode())) {
            return GetTenantRes.builder().id(tenantEntity.getId()).name(tenantEntity.getName()).build();
        }

        // 判断用户是否在租户中
        Optional<TenantUserEntity> tenantUserEntityOptional =
            tenantUserRepository.findByTenantIdAndUserId(getTenantReq.getTenantId(), ContextHolder.getUserId());
        if (!tenantUserEntityOptional.isPresent()) {
            throw new IsxAppException("不在租户中");
        }
        return GetTenantRes.builder().id(tenantEntity.getId()).name(tenantEntity.getName()).build();
    }

    private boolean isInValidTime(TenantEntity tenant) {

        return tenant.getValidStartDateTime() == null || tenant.getValidEndDateTime() == null
            || (!LocalDateTime.now().isBefore(tenant.getValidStartDateTime())
                && !LocalDateTime.now().isAfter(tenant.getValidEndDateTime()));
    }
}
