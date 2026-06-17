package com.isxcode.spark.security.authorization;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleInstancePermissionRepository extends JpaRepository<RoleInstancePermissionEntity, String> {

    Optional<RoleInstancePermissionEntity> findByTenantIdAndRoleIdAndResourceType(String tenantId, String roleId,
        String resourceType);

    List<RoleInstancePermissionEntity> findAllByTenantIdAndRoleId(String tenantId, String roleId);

    void deleteAllByTenantIdAndRoleId(String tenantId, String roleId);
}
