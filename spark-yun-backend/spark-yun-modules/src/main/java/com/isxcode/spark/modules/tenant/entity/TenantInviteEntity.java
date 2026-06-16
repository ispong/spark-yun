package com.isxcode.spark.modules.tenant.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.isxcode.spark.common.jpa.SyId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@Table(name = "sy_tenant_invite_code")
@SQLDelete(sql = "UPDATE sy_tenant_invite_code SET deleted = 1 WHERE id = ? and version_number = ?")
@SQLRestriction("deleted = 0")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
@EntityListeners(AuditingEntityListener.class)
public class TenantInviteEntity {

    @Id
    @SyId
    private String id;

    private String tenantId;

    private String inviteCode;

    private Integer validDays;

    private LocalDateTime expireDateTime;

    private String roleIds;

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
}
