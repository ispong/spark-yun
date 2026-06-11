package com.isxcode.spark.security.authorization;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgRoleRepository extends JpaRepository<OrgRoleEntity, String> {

    List<OrgRoleEntity> findAllByTenantIdAndOrgIdIn(String tenantId, Collection<String> orgIds);

    List<OrgRoleEntity> findAllByTenantIdAndOrgId(String tenantId, String orgId);

    long countByTenantIdAndRoleId(String tenantId, String roleId);

    void deleteAllByTenantIdAndOrgId(String tenantId, String orgId);

    void deleteAllByTenantIdAndRoleId(String tenantId, String roleId);
}
