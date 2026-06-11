package com.isxcode.spark.security.authorization;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgRepository extends JpaRepository<OrgEntity, String> {

    List<OrgEntity> findAllByTenantIdOrderByCreateDateTimeAsc(String tenantId);

    Optional<OrgEntity> findByTenantIdAndName(String tenantId, String name);

    long countByTenantIdAndParentId(String tenantId, String parentId);
}
