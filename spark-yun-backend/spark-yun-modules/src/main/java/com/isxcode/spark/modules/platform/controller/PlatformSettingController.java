package com.isxcode.spark.modules.platform.controller;

import com.isxcode.spark.api.platform.req.UpdatePlatformSettingReq;
import com.isxcode.spark.api.platform.res.GetPlatformSettingRes;
import com.isxcode.spark.api.platform.res.UploadBrandImageRes;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.common.annotations.successResponse.SuccessResponse;
import com.isxcode.spark.common.userlog.UserLog;
import com.isxcode.spark.modules.platform.service.PlatformSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "platform-setting", description = "平台设置模块")
@RestController
@RequestMapping({"/platform-setting", "/api/platform/settings"})
@RequiredArgsConstructor
public class PlatformSettingController {

    private final PlatformSettingService platformSettingService;

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "获取平台设置")
    @PostMapping("/getSetting")
    @SuccessResponse("获取成功")
    public GetPlatformSettingRes getSetting() {

        return platformSettingService.getSetting();
    }

    @Operation(summary = "获取公开平台设置")
    @PostMapping("/open/getSetting")
    @SuccessResponse("获取成功")
    public GetPlatformSettingRes getOpenSetting() {

        return platformSettingService.getSetting();
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "更新平台设置")
    @PostMapping("/updateSetting")
    @SuccessResponse("保存成功")
    @UserLog(moduleCode = "PLATFORM_SETTING", actionCode = "UPDATE")
    public void updateSetting(@Valid @RequestBody UpdatePlatformSettingReq updatePlatformSettingReq) {

        platformSettingService.updateSetting(updatePlatformSettingReq);
    }

    @Secured({RoleType.PLATFORM_SUPER_ADMIN, RoleType.PLATFORM_ADMIN})
    @Operation(summary = "上传品牌图片")
    @PostMapping("/uploadBrandImage")
    @SuccessResponse("上传成功")
    @UserLog(moduleCode = "PLATFORM_SETTING", actionCode = "UPLOAD_BRAND_IMAGE")
    public UploadBrandImageRes uploadBrandImage(@RequestParam("file") MultipartFile file) {

        return platformSettingService.uploadBrandImage(file);
    }
}
