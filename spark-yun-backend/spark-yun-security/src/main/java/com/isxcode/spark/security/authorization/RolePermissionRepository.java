package com.isxcode.spark.security.authorization;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, String> {

    List<RolePermissionEntity> findAllByTenantIdAndRoleIdIn(String tenantId, Collection<String> roleIds);

    List<RolePermissionEntity> findAllByTenantIdAndRoleId(String tenantId, String roleId);

    void deleteAllByTenantIdAndRoleId(String tenantId, String roleId);
}
