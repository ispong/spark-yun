package com.isxcode.spark.modules.tenant.controller;

import com.isxcode.spark.annotation.vip.LicenseApi;
import com.isxcode.spark.api.main.constants.ModuleCode;
import com.isxcode.spark.api.tenant.req.*;
import com.isxcode.spark.api.tenant.res.GetTenantRes;
import com.isxcode.spark.api.tenant.res.PageTenantRes;
import com.isxcode.spark.api.tenant.res.ChooseTenantRes;
import com.isxcode.spark.api.tenant.res.QueryUserTenantRes;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.common.annotations.successResponse.SuccessResponse;
import com.isxcode.spark.common.userlog.UserLog;
import com.isxcode.spark.modules.tenant.service.biz.TenantBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "tenant", description = "租户模块")
@RequestMapping({ModuleCode.TENANT, "/api/platform/tenants"})
@RestController
@RequiredArgsConstructor
public class TenantController {

    private final TenantBizService tenantBizService;

    @LicenseApi
    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "创建租户接口")
    @PostMapping("/addTenant")
    @UserLog(moduleCode = "TENANT", actionCode = "ADD")
    @SuccessResponse("创建成功")
    public void addTenant(@Valid @RequestBody AddTenantReq addTenantReq) {

        tenantBizService.addTenant(addTenantReq);
    }

    @Operation(summary = "查询租户列表接口")
    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @PostMapping("/pageTenant")
    @SuccessResponse("查询成功")
    public Page<PageTenantRes> pageTenant(@Valid @RequestBody PageTenantReq pageTenantReq) {

        return tenantBizService.pageTenant(pageTenantReq);
    }

    @Operation(summary = "查询用户租户列表接口")
    @PostMapping("/queryUserTenant")
    @SuccessResponse("查询成功")
    public List<QueryUserTenantRes> queryUserTenant() {

        return tenantBizService.queryUserTenant();
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "系统管理员更新租户接口")
    @PostMapping("/updateTenantForSystemAdmin")
    @UserLog(moduleCode = "TENANT", actionCode = "UPDATE")
    @SuccessResponse("更新成功")
    public void updateTenantForSystemAdmin(
        @Valid @RequestBody UpdateTenantForSystemAdminReq updateTenantForSystemAdminReq) {

        tenantBizService.updateTenantForSystemAdmin(updateTenantForSystemAdminReq);
    }

    @Secured({RoleType.TENANT_SUPER_ADMIN, RoleType.TENANT_ADMIN})
    @Operation(summary = "租户管理员更新租户接口")
    @PostMapping("/updateTenantForTenantAdmin")
    @SuccessResponse("更新成功")
    public void updateTenantForTenantAdmin(
        @Valid @RequestBody UpdateTenantForTenantAdminReq updateTenantForTenantAdminReq) {

        tenantBizService.updateTenantForTenantAdmin(updateTenantForTenantAdminReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "启动租户接口")
    @PostMapping("/enableTenant")
    @UserLog(moduleCode = "TENANT", actionCode = "ENABLE")
    @SuccessResponse("启用成功")
    public void enableTenant(@Valid @RequestBody EnableTenantReq enableTenantReq) {

        tenantBizService.enableTenant(enableTenantReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "禁用租户接口")
    @PostMapping("/disableTenant")
    @UserLog(moduleCode = "TENANT", actionCode = "DISABLE")
    @SuccessResponse("禁用成功")
    public void disableTenant(@Valid @RequestBody DisableTenantReq disableTenantReq) {

        tenantBizService.disableTenant(disableTenantReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "检测租户信息接口")
    @PostMapping("/checkTenant")
    @SuccessResponse("检测完成")
    public void checkTenant(@Valid @RequestBody CheckTenantReq checkTenantReq) {

        tenantBizService.checkTenant(checkTenantReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "删除租户接口")
    @PostMapping("/deleteTenant")
    @UserLog(moduleCode = "TENANT", actionCode = "DELETE")
    @SuccessResponse("删除成功")
    public void deleteTenant(@Valid @RequestBody DeleteTenantReq deleteTenantReq) {

        tenantBizService.deleteTenant(deleteTenantReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "替换租户管理员接口")
    @PostMapping("/replaceAdmin")
    @UserLog(moduleCode = "TENANT", actionCode = "REPLACE_ADMIN")
    @SuccessResponse("替换成功")
    public void replaceTenantAdmin(@Valid @RequestBody ReplaceTenantAdminReq request) {

        tenantBizService.replaceTenantAdmin(request);
    }

    @Operation(summary = "选择租户接口")
    @PostMapping("/chooseTenant")
    @SuccessResponse("切换成功")
    public ChooseTenantRes chooseTenant(@Valid @RequestBody ChooseTenantReq chooseTenantReq) {

        return tenantBizService.chooseTenant(chooseTenantReq);
    }

    @Operation(summary = "获取租户信息接口")
    @PostMapping("/getTenant")
    @SuccessResponse("获取成功")
    public GetTenantRes getTenant(@Valid @RequestBody GetTenantReq getTenantReq) {

        return tenantBizService.getTenant(getTenantReq);
    }
}
