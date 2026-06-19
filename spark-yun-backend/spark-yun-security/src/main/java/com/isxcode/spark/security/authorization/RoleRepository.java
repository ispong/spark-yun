package com.isxcode.spark.security.authorization;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, String> {

    Optional<RoleEntity> findByTenantIdAndName(String tenantId, String name);

    Optional<RoleEntity> findByTenantIdAndCode(String tenantId, String code);

    List<RoleEntity> findAllByTenantIdAndStatus(String tenantId, String status);

    List<RoleEntity> findAllByTenantIdAndIdIn(String tenantId, Set<String> ids);

    @Query("SELECT R FROM RoleEntity R WHERE R.tenantId = :tenantId "
        + "AND (R.name LIKE %:keyword% OR R.code LIKE %:keyword%) ORDER BY R.createDateTime DESC")
    Page<RoleEntity> search(@Param("tenantId") String tenantId, @Param("keyword") String keyword, Pageable pageable);
}
