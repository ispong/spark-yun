package com.isxcode.spark.security.authorization;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRoleRepository extends JpaRepository<MemberRoleEntity, String> {

    List<MemberRoleEntity> findAllByTenantIdAndUserId(String tenantId, String userId);

    long countByTenantIdAndRoleId(String tenantId, String roleId);

    void deleteAllByTenantIdAndUserId(String tenantId, String userId);

    void deleteAllByTenantIdAndRoleId(String tenantId, String roleId);
}
