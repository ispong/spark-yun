package com.isxcode.star.modules.work.repository;

import com.isxcode.star.api.main.constants.ModuleCode;
import com.isxcode.star.modules.work.entity.WorkEntity;
import com.isxcode.star.modules.work.entity.WorkEventEntity;
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
@CacheConfig(cacheNames = {ModuleCode.WORK})
public interface WorkEventRepository extends JpaRepository<WorkEventEntity, String> {

}
