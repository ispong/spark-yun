package com.isxcode.spark.modules.auth.repository;

import com.isxcode.spark.modules.auth.entity.LoginMethodConfigEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginMethodConfigRepository extends JpaRepository<LoginMethodConfigEntity, String> {

    Optional<LoginMethodConfigEntity> findByConfigKey(String configKey);
}
