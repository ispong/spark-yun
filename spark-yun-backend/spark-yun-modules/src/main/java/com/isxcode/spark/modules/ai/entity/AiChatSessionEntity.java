package com.isxcode.spark.modules.ai.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.isxcode.spark.common.jpa.SyId;
import com.isxcode.spark.common.security.ContextHolder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@Table(name = "sy_ai_chat_session")
@SQLDelete(sql = "UPDATE sy_ai_chat_session SET deleted = 1 WHERE id = ? and version_number = ?")
@SQLRestriction("deleted = 0")
@Filter(name = "tenantFilter", condition = "tenant_id in (:tenantIds)")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
@EntityListeners(AuditingEntityListener.class)
public class AiChatSessionEntity {

    @Id
    @SyId
    private String id;

    private String tenantId;

    private String userId;

    private String configId;

    private String title;

    @Lob
    @Column(name = "messages_json")
    private String messagesJson;

    @CreatedDate
    private LocalDateTime createDateTime;

    @LastModifiedDate
    private LocalDateTime lastModifiedDateTime;

    @CreatedBy
    private String createBy;

    @LastModifiedBy
    private String lastModifiedBy;

    @Version
    private Long versionNumber;

    @Transient
    private Integer deleted;

    @PrePersist
    public void prePersist() {

        tenantId = ContextHolder.getTenantId();
        userId = ContextHolder.getUserId();
    }
}
