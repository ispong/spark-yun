package com.isxcode.spark.modules.ai.repository;

import com.isxcode.spark.modules.ai.entity.AiPromptEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiPromptRepository extends JpaRepository<AiPromptEntity, String> {

    List<AiPromptEntity> findAllByTenantIdOrderByLastModifiedDateTimeDesc(String tenantId);

    Optional<AiPromptEntity> findByTenantIdAndName(String tenantId, String name);
}
