package com.isxcode.spark.common.userlog;

import com.isxcode.spark.api.main.constants.ModuleCode;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@CacheConfig(cacheNames = {ModuleCode.USER})
public interface UserActionRepository extends JpaRepository<UserActionEntity, String> {

    @Query("select U from UserActionEntity U where "
        + "(:moduleCode is null or :moduleCode = '' or U.moduleCode = :moduleCode) and "
        + "(:logType is null or :logType = '' or U.logType = :logType) and "
        + "(:account is null or :account = '' or exists (select 1 from UserEntity SU where SU.id = U.userId and SU.account like %:account%)) and "
        + "(:tenantId is null or :tenantId = '' or U.tenantId = :tenantId) and "
        + "(:startDateTime is null or U.createDateTime >= :startDateTime) and "
        + "(:endDateTime is null or U.createDateTime <= :endDateTime) and "
        + "(U.userId like %:keyword% or U.tenantId like %:keyword% or U.logType like %:keyword% or U.moduleCode like %:keyword% "
        + "or U.moduleName like %:keyword% or U.actionCode like %:keyword% or U.actionName like %:keyword% "
        + "or U.apiName like %:keyword% or U.reqPath like %:keyword% or U.reqMethod like %:keyword% "
        + "or U.ipAddress like %:keyword% or U.userAgent like %:keyword% or U.status like %:keyword% "
        + "or exists (select 1 from UserEntity KU where KU.id = U.userId and KU.account like %:keyword%)) "
        + "order by U.createDateTime desc")
    Page<UserActionEntity> pageLog(@Param("keyword") String searchKeyWord, @Param("moduleCode") String moduleCode,
        @Param("logType") String logType, @Param("account") String account, @Param("tenantId") String tenantId,
        @Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime,
        Pageable pageable);

    @Modifying
    @Query("delete from UserActionEntity U where U.createDateTime < :expireDateTime")
    int deleteExpired(@Param("expireDateTime") LocalDateTime expireDateTime);
}
