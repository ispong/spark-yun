package com.isxcode.spark.security.user;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

    @Query("SELECT U FROM UserEntity U " + "WHERE U.roleCode != 'PLATFORM_SUPER_ADMIN' and " + "(U.username LIKE %:keyword% "
        + "OR U.account LIKE %:keyword% " + "OR U.email LIKE %:keyword% " + "OR U.phone LIKE %:keyword% "
        + "OR U.remark LIKE %:keyword%) order by U.createDateTime desc ")
    Page<UserEntity> searchAllUser(@Param("keyword") String searchKeyWord, Pageable pageable);

    @Query("SELECT U FROM UserEntity U " + "WHERE U.roleCode != 'PLATFORM_SUPER_ADMIN' " + "AND U.status = 'ENABLE' "
        + "AND (U.username LIKE %:keyword%" + " OR U.account LIKE %:keyword% " + "OR U.email LIKE %:keyword% "
        + "OR U.phone LIKE %:keyword% " + "OR U.remark LIKE %:keyword%) order by U.createDateTime desc ")
    Page<UserEntity> searchAllEnableUser(@Param("keyword") String searchKeyWord, Pageable pageable);

    Optional<UserEntity> findByAccount(String account);

    Optional<UserEntity> findByPhone(String phone);

    Optional<UserEntity> findByEmail(String email);

    @Query(value = "SELECT COUNT(*) FROM sy_user WHERE username = :username AND id <> :excludedId", nativeQuery = true)
    long countIncludingDeletedByUsername(@Param("username") String username, @Param("excludedId") String excludedId);

    @Query(value = "SELECT COUNT(*) FROM sy_user WHERE account = :account AND id <> :excludedId", nativeQuery = true)
    long countIncludingDeletedByAccount(@Param("account") String account, @Param("excludedId") String excludedId);

    @Query(value = "SELECT COUNT(*) FROM sy_user WHERE phone = :phone AND id <> :excludedId", nativeQuery = true)
    long countIncludingDeletedByPhone(@Param("phone") String phone, @Param("excludedId") String excludedId);

    @Query(value = "SELECT COUNT(*) FROM sy_user WHERE email = :email AND id <> :excludedId", nativeQuery = true)
    long countIncludingDeletedByEmail(@Param("email") String email, @Param("excludedId") String excludedId);
}
