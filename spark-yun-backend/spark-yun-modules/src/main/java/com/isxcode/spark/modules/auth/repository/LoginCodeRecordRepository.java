package com.isxcode.spark.modules.auth.repository;

import com.isxcode.spark.modules.auth.entity.LoginCodeRecordEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginCodeRecordRepository extends JpaRepository<LoginCodeRecordEntity, String> {

    Optional<LoginCodeRecordEntity> findFirstByChannelAndReceiverAndSceneOrderByCreateDateTimeDesc(String channel,
        String receiver, String scene);

    Optional<LoginCodeRecordEntity> findFirstByChannelAndReceiverAndSceneAndSendStatusOrderByCreateDateTimeDesc(
        String channel, String receiver, String scene, String sendStatus);

    @Query("select R from LoginCodeRecordEntity R where "
        + "(:channel is null or :channel = '' or R.channel = :channel) and "
        + "(R.receiver like %:keyword% or R.channel like %:keyword% or R.sendStatus like %:keyword% "
        + "or R.verifyStatus like %:keyword% or R.errorMessage like %:keyword%) order by R.createDateTime desc")
    Page<LoginCodeRecordEntity> pageRecord(@Param("keyword") String searchKeyWord, @Param("channel") String channel,
        Pageable pageable);
}
