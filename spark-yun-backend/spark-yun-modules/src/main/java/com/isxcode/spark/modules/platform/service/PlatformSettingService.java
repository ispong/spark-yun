package com.isxcode.spark.modules.platform.service;

import com.isxcode.spark.api.platform.req.UpdatePlatformSettingReq;
import com.isxcode.spark.api.platform.res.GetPlatformSettingRes;
import com.isxcode.spark.common.security.ContextHolder;
import com.isxcode.spark.modules.platform.entity.PlatformSettingEntity;
import com.isxcode.spark.modules.platform.repository.PlatformSettingRepository;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PlatformSettingService {

    public static final String GLOBAL_SETTING_KEY = "GLOBAL";

    private static final String SYSTEM_USER = "system";

    private final PlatformSettingRepository platformSettingRepository;

    public GetPlatformSettingRes getSetting() {

        PlatformSettingEntity setting = getOrCreateSetting();
        return GetPlatformSettingRes.builder().description(setting.getDescription()).build();
    }

    public void updateSetting(UpdatePlatformSettingReq updatePlatformSettingReq) {

        PlatformSettingEntity setting = getOrCreateSetting();
        setting.setDescription(updatePlatformSettingReq.getDescription());
        platformSettingRepository.save(setting);
    }

    private PlatformSettingEntity getOrCreateSetting() {

        return platformSettingRepository.findBySettingKey(GLOBAL_SETTING_KEY)
            .orElseGet(() -> withSystemUser(() -> platformSettingRepository.save(defaultSettingEntity())));
    }

    private PlatformSettingEntity defaultSettingEntity() {

        PlatformSettingEntity setting = new PlatformSettingEntity();
        setting.setSettingKey(GLOBAL_SETTING_KEY);
        setting.setDescription("");
        return setting;
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
