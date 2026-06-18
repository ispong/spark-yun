package com.isxcode.spark.modules.platform.service;

import com.isxcode.spark.api.platform.req.UpdatePlatformSettingReq;
import com.isxcode.spark.api.platform.res.GetPlatformSettingRes;
import com.isxcode.spark.api.platform.res.UploadBrandImageRes;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.common.security.ContextHolder;
import com.isxcode.spark.modules.platform.entity.PlatformSettingEntity;
import com.isxcode.spark.modules.platform.repository.PlatformSettingRepository;
import java.io.IOException;
import java.util.Base64;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PlatformSettingService {

    public static final String GLOBAL_SETTING_KEY = "GLOBAL";

    private static final String SYSTEM_USER = "system";

    private static final long MAX_BRAND_IMAGE_SIZE = 2 * 1024 * 1024;

    private final PlatformSettingRepository platformSettingRepository;

    public GetPlatformSettingRes getSetting() {

        PlatformSettingEntity setting = getOrCreateSetting();
        return GetPlatformSettingRes.builder().description(setting.getDescription())
            .autoCreateTenant(valueOrDefault(setting.getAutoCreateTenant(), false))
            .browserTitle(setting.getBrowserTitle()).themeColor(setting.getThemeColor())
            .faviconUrl(setting.getFaviconUrl()).topLogoUrl(setting.getTopLogoUrl())
            .topLogoSmallUrl(setting.getTopLogoSmallUrl()).loginMainImageUrl(setting.getLoginMainImageUrl()).build();
    }

    public void updateSetting(UpdatePlatformSettingReq updatePlatformSettingReq) {

        PlatformSettingEntity setting = getOrCreateSetting();
        setting.setDescription(updatePlatformSettingReq.getDescription());
        setting.setAutoCreateTenant(valueOrDefault(updatePlatformSettingReq.getAutoCreateTenant(),
            valueOrDefault(setting.getAutoCreateTenant(), false)));
        setting.setBrowserTitle(updatePlatformSettingReq.getBrowserTitle());
        setting.setThemeColor(updatePlatformSettingReq.getThemeColor());
        setting.setFaviconUrl(updatePlatformSettingReq.getFaviconUrl());
        setting.setTopLogoUrl(updatePlatformSettingReq.getTopLogoUrl());
        setting.setTopLogoSmallUrl(updatePlatformSettingReq.getTopLogoSmallUrl());
        setting.setLoginMainImageUrl(updatePlatformSettingReq.getLoginMainImageUrl());
        platformSettingRepository.save(setting);
    }

    public UploadBrandImageRes uploadBrandImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IsxAppException("请上传图片文件");
        }
        if (file.getSize() > MAX_BRAND_IMAGE_SIZE) {
            throw new IsxAppException("图片大小不能超过2MB");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        boolean isIcon = originalFilename.endsWith(".ico");
        if ((contentType == null || !contentType.startsWith("image/")) && !isIcon) {
            throw new IsxAppException("请上传图片文件");
        }

        try {
            String mimeType = contentType == null || contentType.isBlank() ? "image/x-icon" : contentType;
            String dataUrl = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
            return UploadBrandImageRes.builder().url(dataUrl).build();
        } catch (IOException e) {
            throw new IsxAppException("图片上传失败");
        }
    }

    private PlatformSettingEntity getOrCreateSetting() {

        return platformSettingRepository.findBySettingKey(GLOBAL_SETTING_KEY)
            .orElseGet(() -> withSystemUser(() -> platformSettingRepository.save(defaultSettingEntity())));
    }

    private PlatformSettingEntity defaultSettingEntity() {

        PlatformSettingEntity setting = new PlatformSettingEntity();
        setting.setSettingKey(GLOBAL_SETTING_KEY);
        setting.setDescription("");
        setting.setAutoCreateTenant(false);
        setting.setBrowserTitle("");
        setting.setThemeColor("");
        return setting;
    }

    private boolean valueOrDefault(Boolean value, boolean defaultValue) {

        return value == null ? defaultValue : value;
    }

    private <T> T withSystemUser(Supplier<T> supplier) {

        boolean needsContext = Strings.isEmpty(ContextHolder.getUserId());
        if (needsContext) {
            ContextHolder.setCurrentUser(SYSTEM_USER, null);
        }
        try {
            return supplier.get();
        } finally {
            if (needsContext) {
                ContextHolder.clear();
            }
        }
    }
}
