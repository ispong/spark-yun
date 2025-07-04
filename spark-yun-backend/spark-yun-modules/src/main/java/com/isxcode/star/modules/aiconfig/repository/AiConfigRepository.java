package com.isxcode.star.modules.aiconfig.repository;

import com.isxcode.star.api.main.constants.ModuleCode;
import com.isxcode.star.modules.aiconfig.entity.AiConfigEntity;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@CacheConfig(cacheNames = {ModuleCode.AI_CONFIG})
public interface AiConfigRepository extends JpaRepository<AiConfigEntity, String> {

    Optional<AiConfigEntity> findByName(String name);

    List<AiConfigEntity> findAllByStatus(String status);

    @Query("SELECT A FROM AiConfigEntity A WHERE A.name LIKE %:keyword% OR A.remark LIKE %:keyword% order by A.createDateTime desc ")
    Page<AiConfigEntity> searchAll(@Param("keyword") String searchKeyWord, Pageable pageable);
}
