package com.isxcode.spark.security.authorization;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleInstancePermissionRepository extends JpaRepository<RoleInstancePermissionEntity, String> {

    Optional<RoleInstancePermissionEntity> findByTenantIdAndRoleIdAndResourceType(String tenantId, String roleId,
        String resourceType);

    List<RoleInstancePermissionEntity> findAllByTenantIdAndRoleIdIn(String tenantId, Set<String> roleIds);

    List<RoleInstancePermissionEntity> findAllByTenantIdAndRoleId(String tenantId, String roleId);

    void deleteAllByTenantIdAndRoleId(String tenantId, String roleId);
}
