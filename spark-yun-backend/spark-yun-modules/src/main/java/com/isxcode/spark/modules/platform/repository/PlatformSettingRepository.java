package com.isxcode.spark.modules.platform.repository;

import com.isxcode.spark.modules.platform.entity.PlatformSettingEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformSettingRepository extends JpaRepository<PlatformSettingEntity, String> {

    Optional<PlatformSettingEntity> findBySettingKey(String settingKey);
}
