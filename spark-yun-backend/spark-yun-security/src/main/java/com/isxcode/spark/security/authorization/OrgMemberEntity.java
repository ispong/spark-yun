package com.isxcode.spark.security.authorization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.isxcode.spark.common.jpa.SyId;
import com.isxcode.spark.common.security.ContextHolder;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@Table(name = "sy_org_member")
@SQLDelete(sql = "UPDATE sy_org_member SET deleted = 1 WHERE id = ? and version_number = ?")
@SQLRestriction("deleted = 0")
@Filter(name = "tenantFilter", condition = "tenant_id in (:tenantIds)")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
@EntityListeners(AuditingEntityListener.class)
public class OrgMemberEntity {

    @Id
    @SyId
    private String id;

    private String tenantId;

    private String orgId;

    private String userId;

    @CreatedDate
    private LocalDateTime createDateTime;

    @CreatedBy
    private String createBy;

    @Version
    private Long versionNumber;

    @Transient
    private Integer deleted;

    @PrePersist
    public void prePersist() {

        tenantId = ContextHolder.getTenantId();
    }
}
