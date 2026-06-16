package com.isxcode.spark.modules.auth.repository;

import com.isxcode.spark.modules.auth.entity.LoginLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLogEntity, String> {

    @Query("select L from LoginLogEntity L where "
        + "(:loginMethod is null or :loginMethod = '' or L.loginMethod = :loginMethod) and "
        + "(:loginStatus is null or :loginStatus = '' or L.loginStatus = :loginStatus) and "
        + "(L.accountIdentifier like %:keyword% or L.loginMethod like %:keyword% "
        + "or L.ipAddress like %:keyword% or L.userAgent like %:keyword% or L.loginStatus like %:keyword% "
        + "or L.errorMessage like %:keyword%) order by L.createDateTime desc")
    Page<LoginLogEntity> pageLog(@Param("keyword") String searchKeyWord, @Param("loginMethod") String loginMethod,
        @Param("loginStatus") String loginStatus, Pageable pageable);
}
