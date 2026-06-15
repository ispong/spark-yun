package com.isxcode.spark.modules.auth.controller;

import com.isxcode.spark.api.auth.req.PageLoginCodeRecordReq;
import com.isxcode.spark.api.auth.req.SendLoginCodeReq;
import com.isxcode.spark.api.auth.req.UpdateLoginMethodConfigReq;
import com.isxcode.spark.api.auth.req.VerifyLoginCodeReq;
import com.isxcode.spark.api.auth.res.GetLoginMethodConfigRes;
import com.isxcode.spark.api.auth.res.GetOpenLoginMethodConfigRes;
import com.isxcode.spark.api.auth.res.PageLoginCodeRecordRes;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.api.user.res.LoginRes;
import com.isxcode.spark.common.annotations.successResponse.SuccessResponse;
import com.isxcode.spark.modules.auth.service.LoginMethodBizService;
import com.isxcode.spark.modules.auth.service.LoginMethodConfigService;
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

@Tag(name = "login-method", description = "登录方式模块")
@RestController
@RequestMapping({"/login-method", "/api/platform/login-method"})
@RequiredArgsConstructor
public class LoginMethodController {

    private final LoginMethodConfigService loginMethodConfigService;

    private final LoginMethodBizService loginMethodBizService;

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "获取登录方式配置")
    @PostMapping("/getConfig")
    @SuccessResponse("获取成功")
    public GetLoginMethodConfigRes getConfig() {

        return loginMethodConfigService.getConfig();
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "更新登录方式配置")
    @PostMapping("/updateConfig")
    @SuccessResponse("保存成功")
    public void updateConfig(@Valid @RequestBody UpdateLoginMethodConfigReq updateLoginMethodConfigReq) {

        loginMethodConfigService.updateConfig(updateLoginMethodConfigReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "分页查询验证码发送记录")
    @PostMapping("/pageRecord")
    @SuccessResponse("查询成功")
    public Page<PageLoginCodeRecordRes> pageRecord(@Valid @RequestBody PageLoginCodeRecordReq pageLoginCodeRecordReq) {

        return loginMethodBizService.pageRecord(pageLoginCodeRecordReq);
    }

    @Operation(summary = "获取开放登录方式配置")
    @PostMapping("/open/getConfig")
    @SuccessResponse("获取成功")
    public GetOpenLoginMethodConfigRes getOpenConfig() {

        return loginMethodConfigService.getOpenConfig();
    }

    @Operation(summary = "发送登录验证码")
    @PostMapping("/open/sendCode")
    @SuccessResponse("发送成功")
    public void sendCode(@Valid @RequestBody SendLoginCodeReq sendLoginCodeReq) {

        loginMethodBizService.sendCode(sendLoginCodeReq);
    }

    @Operation(summary = "验证码登录")
    @PostMapping("/open/verifyLogin")
    @SuccessResponse("登录成功")
    public LoginRes verifyLogin(@Valid @RequestBody VerifyLoginCodeReq verifyLoginCodeReq) {

        return loginMethodBizService.verifyLogin(verifyLoginCodeReq);
    }
}
