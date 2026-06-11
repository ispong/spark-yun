package com.isxcode.spark.security.authorization;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgMemberRepository extends JpaRepository<OrgMemberEntity, String> {

    List<OrgMemberEntity> findAllByTenantIdAndUserId(String tenantId, String userId);

    List<OrgMemberEntity> findAllByTenantIdAndOrgId(String tenantId, String orgId);

    void deleteAllByTenantIdAndOrgId(String tenantId, String orgId);

    void deleteAllByTenantIdAndUserId(String tenantId, String userId);

    void deleteAllByTenantIdAndUserIdIn(String tenantId, Collection<String> userIds);
}
