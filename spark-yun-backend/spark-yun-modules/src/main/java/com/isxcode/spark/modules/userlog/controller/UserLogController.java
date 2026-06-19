package com.isxcode.spark.modules.userlog.controller;

import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.api.userlog.req.PageUserLogReq;
import com.isxcode.spark.api.userlog.res.PageUserLogRes;
import com.isxcode.spark.api.userlog.res.UserLogDefinitionRes;
import com.isxcode.spark.common.annotations.successResponse.SuccessResponse;
import com.isxcode.spark.modules.userlog.service.UserLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "user-log", description = "行为日志模块")
@RestController
@RequestMapping({"/user-log", "/api/platform/user-log"})
@RequiredArgsConstructor
public class UserLogController {

    private final UserLogService userLogService;

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "分页查询行为日志")
    @PostMapping("/page")
    @SuccessResponse("查询成功")
    public Page<PageUserLogRes> page(@Valid @RequestBody PageUserLogReq request) {

        return userLogService.pageLog(request);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "查询行为日志字典")
    @PostMapping("/definitions")
    @SuccessResponse("查询成功")
    public List<UserLogDefinitionRes> definitions() {

        return userLogService.definitions();
    }
}
