package com.isxcode.spark.modules.ai.repository;

import com.isxcode.spark.modules.ai.entity.AiConfigEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AiConfigRepository extends JpaRepository<AiConfigEntity, String> {

    Optional<AiConfigEntity> findByTenantIdAndName(String tenantId, String name);

    List<AiConfigEntity> findAllByTenantIdAndStatusOrderByCreateDateTimeDesc(String tenantId, String status);

    @Query("SELECT A FROM AiConfigEntity A WHERE A.tenantId = :tenantId "
        + "AND (A.name LIKE %:keyword% OR A.providerType LIKE %:keyword% OR A.modelName LIKE %:keyword%) "
        + "ORDER BY A.createDateTime DESC")
    Page<AiConfigEntity> search(@Param("tenantId") String tenantId, @Param("keyword") String keyword,
        Pageable pageable);
}
