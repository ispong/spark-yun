package com.isxcode.spark.modules.user.controller;

import com.isxcode.spark.annotation.vip.LicenseApi;
import com.isxcode.spark.api.main.constants.ModuleCode;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.api.user.req.*;
import com.isxcode.spark.api.user.res.*;
import com.isxcode.spark.common.annotations.successResponse.SuccessResponse;
import com.isxcode.spark.common.userlog.UserLog;
import com.isxcode.spark.modules.auth.service.LoginMethodBizService;
import com.isxcode.spark.modules.user.service.UserBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;


@Tag(name = "user", description = "用户模块")
@RequestMapping({ModuleCode.USER, "/api/platform/users"})
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserBizService userBizService;

    private final LoginMethodBizService loginMethodBizService;

    @Operation(summary = "用户登录接口")
    @PostMapping("/open/login")
    @SuccessResponse("登录成功")
    public LoginRes login(@Valid @RequestBody LoginReq loginReq) {

        return userBizService.login(loginReq);
    }

    @Operation(summary = "刷新token接口")
    @PostMapping("/open/refreshToken")
    @SuccessResponse("刷新成功")
    public LoginRes refreshToken(@Valid @RequestBody RefreshTokenReq refreshTokenReq) {

        return userBizService.refreshToken(refreshTokenReq);
    }

    @Operation(summary = "用户退出接口")
    @PostMapping("/logout")
    @SuccessResponse("退出成功")
    public void logout() {

        userBizService.logout();
    }

    @Operation(summary = "获取用户信息接口")
    @PostMapping("/getUser")
    @SuccessResponse("获取成功")
    public GetUserRes getUser() {

        return userBizService.getUser();
    }

    @LicenseApi
    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "创建用户接口")
    @PostMapping("/addUser")
    @UserLog(moduleCode = "USER", actionCode = "ADD")
    @SuccessResponse("创建成功")
    public void addUser(@Valid @RequestBody AddUserReq addUserReq) {

        userBizService.addUser(addUserReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "更新用户接口")
    @PostMapping("/updateUser")
    @UserLog(moduleCode = "USER", actionCode = "UPDATE")
    @SuccessResponse("更新成功")
    public void updateUser(@Valid @RequestBody UpdateUserReq updateUserReq) {

        userBizService.updateUser(updateUserReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "修改用户密码接口")
    @PostMapping("/updateUserPassword")
    @SuccessResponse("修改成功")
    public void updateUserPassword(@Valid @RequestBody UpdateUserPasswordReq updateUserPasswordReq) {

        userBizService.updateUserPassword(updateUserPasswordReq);
    }

    @Operation(summary = "更新用户信息接口")
    @PostMapping("/updateUserInfo")
    @SuccessResponse("更新成功")
    public void updateUserInfo(@Valid @RequestBody UpdateUserInfoReq updateUserInfoReq) {

        userBizService.updateUserInfo(updateUserInfoReq);
    }

    @Operation(summary = "修改个人密码接口")
    @PostMapping("/updateMyPassword")
    @SuccessResponse("修改成功")
    public void updateMyPassword(@Valid @RequestBody UpdateMyPasswordReq updateMyPasswordReq) {

        loginMethodBizService.updateMyPassword(updateMyPasswordReq);
    }

    @Operation(summary = "发送修改密码验证码接口")
    @PostMapping("/sendUpdatePasswordCode")
    @SuccessResponse("发送成功")
    public void sendUpdatePasswordCode(@Valid @RequestBody SendUpdatePasswordCodeReq sendUpdatePasswordCodeReq) {

        loginMethodBizService.sendUpdatePasswordCode(sendUpdatePasswordCodeReq);
    }

    @Operation(summary = "发送修改手机验证码接口")
    @PostMapping("/sendUpdatePhoneCode")
    @SuccessResponse("发送成功")
    public void sendUpdatePhoneCode(@Valid @RequestBody SendUpdatePhoneCodeReq sendUpdatePhoneCodeReq) {

        loginMethodBizService.sendUpdatePhoneCode(sendUpdatePhoneCodeReq);
    }

    @Operation(summary = "发送修改邮箱验证码接口")
    @PostMapping("/sendUpdateEmailCode")
    @SuccessResponse("发送成功")
    public void sendUpdateEmailCode(@Valid @RequestBody SendUpdateEmailCodeReq sendUpdateEmailCodeReq) {

        loginMethodBizService.sendUpdateEmailCode(sendUpdateEmailCodeReq);
    }

    @Operation(summary = "修改个人手机接口")
    @PostMapping("/updateMyPhone")
    @SuccessResponse("修改成功")
    public void updateMyPhone(@Valid @RequestBody UpdateMyPhoneReq updateMyPhoneReq) {

        loginMethodBizService.updateMyPhone(updateMyPhoneReq);
    }

    @Operation(summary = "修改个人邮箱接口")
    @PostMapping("/updateMyEmail")
    @SuccessResponse("修改成功")
    public void updateMyEmail(@Valid @RequestBody UpdateMyEmailReq updateMyEmailReq) {

        loginMethodBizService.updateMyEmail(updateMyEmailReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "禁用用户接口")
    @PostMapping("/disableUser")
    @UserLog(moduleCode = "USER", actionCode = "DISABLE")
    @SuccessResponse("禁用成功")
    public void disableUser(@Valid @RequestBody DisableUserReq disableUserReq) {

        userBizService.disableUser(disableUserReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "启用用户接口")
    @PostMapping("/enableUser")
    @UserLog(moduleCode = "USER", actionCode = "ENABLE")
    @SuccessResponse("启用成功")
    public void enableUser(@Valid @RequestBody EnableUserReq enableUserReq) {

        userBizService.enableUser(enableUserReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "删除用户接口")
    @PostMapping("/deleteUser")
    @UserLog(moduleCode = "USER", actionCode = "DELETE")
    @SuccessResponse("删除成功")
    public void deleteUser(@Valid @RequestBody DeleteUserReq deleteUserReq) {

        userBizService.deleteUser(deleteUserReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "查询所有用户接口")
    @PostMapping("/pageUser")
    @SuccessResponse("查询成功")
    public Page<PageUserRes> pageUser(@Valid @RequestBody PageUserReq pageUserReq) {

        return userBizService.pageUser(pageUserReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN, RoleType.TENANT_SUPER_ADMIN,
            RoleType.TENANT_ADMIN})
    @Operation(summary = "查询所有启用用户接口")
    @PostMapping("/pageEnableUser")
    @SuccessResponse("查询成功")
    public Page<PageEnableUserRes> pageEnableUser(@Valid @RequestBody PageEnableUserReq pageEnableUserReq) {

        return userBizService.pageEnableUser(pageEnableUserReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.TENANT_SUPER_ADMIN, RoleType.TENANT_ADMIN})
    @Operation(summary = "获取匿名者访问token接口")
    @PostMapping("/getAnonymousToken")
    @SuccessResponse("查询成功")
    public GetAnonymousTokenRes getAnonymousToken(@Valid @RequestBody GetAnonymousTokenReq getAnonymousTokenReq) {

        return userBizService.getAnonymousToken(getAnonymousTokenReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "设置平台管理员接口")
    @PostMapping("/setPlatformAdmin")
    @UserLog(moduleCode = "USER", actionCode = "SET_PLATFORM_ADMIN")
    @SuccessResponse("设置成功")
    public void setPlatformAdmin(@Valid @RequestBody SetPlatformAdminReq setPlatformAdminReq) {

        userBizService.setPlatformAdmin(setPlatformAdminReq);
    }
}
