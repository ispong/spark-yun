package com.isxcode.spark.modules.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.isxcode.spark.common.jpa.SyId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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
@SQLDelete(sql = "UPDATE sy_platform_setting SET deleted = 1 WHERE id = ? and version_number = ?")
@SQLRestriction("deleted = 0")
@Table(name = "sy_platform_setting")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
@EntityListeners(AuditingEntityListener.class)
public class PlatformSettingEntity {

    @Id
    @SyId
    private String id;

    private String settingKey;

    @Column(length = 2000)
    private String description;

    private Boolean autoCreateTenant;

    @Column(length = 100)
    private String browserTitle;

    @Column(length = 20)
    private String themeColor;

    @Lob
    private String faviconUrl;

    @Lob
    private String topLogoUrl;

    @Lob
    private String topLogoSmallUrl;

    @Lob
    private String loginMainImageUrl;

    private Boolean userLogEnabled;

    private Integer userLogRetentionDays;

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
