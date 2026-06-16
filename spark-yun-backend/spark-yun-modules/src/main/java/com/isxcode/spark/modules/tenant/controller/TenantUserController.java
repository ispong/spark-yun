package com.isxcode.spark.modules.tenant.controller;

import com.isxcode.spark.annotation.vip.LicenseApi;
import com.isxcode.spark.api.main.constants.ModuleCode;
import com.isxcode.spark.api.tenant.req.*;
import com.isxcode.spark.api.tenant.res.PageTenantUserRes;
import com.isxcode.spark.api.tenant.res.TenantInviteRes;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.common.annotations.successResponse.SuccessResponse;
import com.isxcode.spark.common.userlog.UserLog;
import com.isxcode.spark.modules.tenant.service.biz.TenantUserBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "tenant-user", description = "租户用户模块")
@RequestMapping({ModuleCode.TENANT_USER, "/api/admin/members"})
@RestController
@RequiredArgsConstructor
public class TenantUserController {

    private final TenantUserBizService tenantUserBizService;

    @LicenseApi
    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN, RoleType.TENANT_SUPER_ADMIN,
            RoleType.TENANT_ADMIN})
    @Operation(summary = "添加用户接口")
    @PostMapping("/addTenantUser")
    @SuccessResponse("添加成功")
    public void addTenantUser(@Valid @RequestBody AddTenantUserReq addTenantUserReq) {

        tenantUserBizService.addTenantUser(addTenantUserReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN, RoleType.TENANT_SUPER_ADMIN,
            RoleType.TENANT_ADMIN})
    @Operation(summary = "查询租户用户列表接口")
    @PostMapping("/pageTenantUser")
    @SuccessResponse("查询成功")
    public Page<PageTenantUserRes> pageTenantUser(@Valid @RequestBody PageTenantUserReq pageTenantUserReq) {

        return tenantUserBizService.pageTenantUser(pageTenantUserReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN, RoleType.TENANT_SUPER_ADMIN,
            RoleType.TENANT_ADMIN})
    @Operation(summary = "移除用户接口")
    @PostMapping("/removeTenantUser")
    @UserLog
    @SuccessResponse("移除成功")
    public void removeTenantUser(@Valid @RequestBody RemoveTenantUserReq removeTenantUserReq) {

        tenantUserBizService.removeTenantUser(removeTenantUserReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN, RoleType.TENANT_SUPER_ADMIN,
            RoleType.TENANT_ADMIN})
    @Operation(summary = "设置为普通管理员接口")
    @PostMapping("/setTenantAdmin")
    @UserLog
    @SuccessResponse("设置成功")
    public void setTenantAdmin(@Valid @RequestBody SetTenantAdminReq setTenantAdminReq) {

        tenantUserBizService.setTenantAdmin(setTenantAdminReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN, RoleType.TENANT_SUPER_ADMIN,
            RoleType.TENANT_ADMIN})
    @Operation(summary = "取消普通管理员接口")
    @PostMapping("/removeTenantAdmin")
    @UserLog
    @SuccessResponse("设置成功")
    public void removeTenantAdmin(@Valid @RequestBody RemoveTenantAdminReq removeTenantAdminReq) {

        tenantUserBizService.removeTenantAdmin(removeTenantAdminReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN, RoleType.TENANT_SUPER_ADMIN,
            RoleType.TENANT_ADMIN})
    @Operation(summary = "修改成员状态接口")
    @PostMapping("/setStatus")
    @UserLog
    @SuccessResponse("设置成功")
    public void setTenantMemberStatus(@Valid @RequestBody SetTenantMemberStatusReq request) {

        tenantUserBizService.setTenantMemberStatus(request);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN, RoleType.TENANT_SUPER_ADMIN,
            RoleType.TENANT_ADMIN})
    @Operation(summary = "设置成员角色接口")
    @PostMapping("/setRoles")
    @UserLog
    @SuccessResponse("设置成功")
    public void setMemberRoles(@Valid @RequestBody SetMemberRolesReq request) {

        tenantUserBizService.setMemberRoles(request);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN, RoleType.TENANT_SUPER_ADMIN,
            RoleType.TENANT_ADMIN})
    @Operation(summary = "获取租户邀请码接口")
    @PostMapping("/getInviteCode")
    @SuccessResponse("查询成功")
    public TenantInviteRes getTenantInvite(@Valid @RequestBody GetTenantInviteReq request) {

        return tenantUserBizService.getTenantInvite(request);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN, RoleType.TENANT_SUPER_ADMIN,
            RoleType.TENANT_ADMIN})
    @Operation(summary = "保存租户邀请码接口")
    @PostMapping("/saveInviteCode")
    @SuccessResponse("保存成功")
    public TenantInviteRes saveTenantInvite(@Valid @RequestBody SaveTenantInviteReq request) {

        return tenantUserBizService.saveTenantInvite(request);
    }

    @Secured({RoleType.PLATFORM_ADMIN, RoleType.PLATFORM_MEMBER, RoleType.TENANT_SUPER_ADMIN, RoleType.TENANT_ADMIN,
            RoleType.TENANT_MEMBER})
    @Operation(summary = "申请加入租户接口")
    @PostMapping("/applyInviteCode")
    @SuccessResponse("申请提交成功")
    public void applyTenantInvite(@Valid @RequestBody ApplyTenantInviteReq request) {

        tenantUserBizService.applyTenantInvite(request);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN, RoleType.TENANT_SUPER_ADMIN,
            RoleType.TENANT_ADMIN})
    @Operation(summary = "通过租户申请接口")
    @PostMapping("/approveApply")
    @UserLog
    @SuccessResponse("通过成功")
    public void approveTenantApply(@Valid @RequestBody ReviewTenantApplyReq request) {

        tenantUserBizService.approveTenantApply(request);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN, RoleType.TENANT_SUPER_ADMIN,
            RoleType.TENANT_ADMIN})
    @Operation(summary = "拒绝租户申请接口")
    @PostMapping("/rejectApply")
    @UserLog
    @SuccessResponse("拒绝成功")
    public void rejectTenantApply(@Valid @RequestBody ReviewTenantApplyReq request) {

        tenantUserBizService.rejectTenantApply(request);
    }
}
