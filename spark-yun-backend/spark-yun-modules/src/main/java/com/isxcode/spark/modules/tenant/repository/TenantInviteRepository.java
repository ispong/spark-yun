package com.isxcode.spark.modules.tenant.repository;

import com.isxcode.spark.modules.tenant.entity.TenantInviteEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantInviteRepository extends JpaRepository<TenantInviteEntity, String> {

    Optional<TenantInviteEntity> findFirstByTenantIdOrderByCreateDateTimeDesc(String tenantId);

    Optional<TenantInviteEntity> findByInviteCode(String inviteCode);
}
