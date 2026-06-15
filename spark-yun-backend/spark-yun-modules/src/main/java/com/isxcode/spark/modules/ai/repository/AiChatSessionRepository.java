package com.isxcode.spark.modules.ai.repository;

import com.isxcode.spark.modules.ai.entity.AiChatSessionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiChatSessionRepository extends JpaRepository<AiChatSessionEntity, String> {

    List<AiChatSessionEntity> findAllByTenantIdAndUserIdOrderByLastModifiedDateTimeDesc(String tenantId, String userId);
}
