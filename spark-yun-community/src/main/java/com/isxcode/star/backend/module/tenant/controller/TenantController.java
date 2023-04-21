package com.isxcode.star.backend.module.tenant.controller;

import com.isxcode.star.api.constants.ModulePrefix;
import com.isxcode.star.api.constants.Roles;
import com.isxcode.star.api.pojos.tenant.req.TetAddTenantReq;
import com.isxcode.star.api.pojos.tenant.req.TetAddUserReq;
import com.isxcode.star.api.pojos.tenant.req.TetUpdateTenantReq;
import com.isxcode.star.api.pojos.tenant.req.TetUpdateUserReq;
import com.isxcode.star.api.response.SuccessResponse;
import com.isxcode.star.backend.module.tenant.service.TenantBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Tag(name = "租户模块")
@RestController
@RequestMapping(ModulePrefix.TENANT)
@RequiredArgsConstructor
public class TenantController {

  private final TenantBizService tenantBizService;

  @Operation(summary = "创建租户接口")
  @PostMapping("/addTenant")
  @SuccessResponse("创建成功")
  public void addTenant(@Valid @RequestBody TetAddTenantReq tetAddTenantReq) {

    tenantBizService.addTenant(tetAddTenantReq);
  }

  @Operation(summary = "更新租户")
  @PostMapping("/updateTenant")
  @SuccessResponse("更新成功")
  public void updateTenant(@Valid @RequestBody TetUpdateTenantReq tetUpdateTenantReq) {

    tenantBizService.updateTenant(tetUpdateTenantReq);
  }

  @Operation(summary = "启动租户")
  @PostMapping("/enableTenant")
  @SuccessResponse("启用成功")
  public void enableTenant(@Schema(description = "租户唯一id", example = "sy_344c3d583fa344f7a2403b19c5a654dc") @RequestParam String tenantId) {

    tenantBizService.enableTenant(tenantId);
  }

  @Operation(summary = "禁用租户")
  @GetMapping("/disableTenant")
  @SuccessResponse("禁用成功")
  public void disableTenant(@Schema(description = "租户唯一id", example = "sy_344c3d583fa344f7a2403b19c5a654dc") @RequestParam String tenantId) {

    tenantBizService.disableTenant(tenantId);
  }

  @Operation(summary = "检测租户")
  @PostMapping("/checkTenant")
  @SuccessResponse("检测完成")
  public void checkTenant(@Schema(description = "租户唯一id", example = "sy_344c3d583fa344f7a2403b19c5a654dc") @RequestParam String tenantId) {

    tenantBizService.checkTenant(tenantId);
  }

  @Secured({Roles.SYS_ADMIN})
  @Operation(summary = "创建用户接口")
  @PostMapping("/addUser")
  @SuccessResponse("创建成功")
  public void addUser(@Valid @RequestBody TetAddUserReq tetAddUserReq) {

    tenantBizService.addUser(tetAddUserReq);
  }

  @Secured({Roles.SYS_ADMIN})
  @Operation(summary = "更新用户接口")
  @PostMapping("/updateUser")
  @SuccessResponse("更新成功")
  public void updateUser(@Valid @RequestBody TetUpdateUserReq tetUpdateUserReq) {

    tenantBizService.updateUser(tetUpdateUserReq);
  }

  @Operation(summary = "禁用用户接口")
  @PostMapping("/disableUser")
  @SuccessResponse("禁用成功")
  public void disableUser(@Schema(description = "用户唯一id", example = "sy_b0288cadb2ab4325ae519ff329a95cda")  @RequestParam String userId) {

    tenantBizService.disableUser(userId);
  }

  @Operation(summary = "删除用户接口")
  @PostMapping("/deleteUser")
  @SuccessResponse("删除成功")
  public void deleteUser(@Schema(description = "用户唯一id", example = "sy_ff3c1b52f8b34c45ab2cf24b6bccd480")  @RequestParam String userId) {

    tenantBizService.deleteUser(userId);
  }

  @Operation(summary = "启用用户接口")
  @PostMapping("/enableUser")
  @SuccessResponse("启用成功")
  public void enableUser(@Schema(description = "用户唯一id", example = "sy_b0288cadb2ab4325ae519ff329a95cda") @RequestParam String userId) {

    tenantBizService.enableUser(userId);
  }
}

