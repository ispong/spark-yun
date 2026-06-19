package com.isxcode.spark.common.userlog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import com.isxcode.spark.common.jpa.SyId;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@Table(name = "sy_user_action")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
@EntityListeners(AuditingEntityListener.class)
public class UserActionEntity {

    @Id
    @SyId
    private String id;

    private String userId;

    private String tenantId;

    private String moduleCode;

    private String moduleName;

    private String logType;

    private String actionCode;

    private String actionName;

    private String apiName;

    private String reqPath;

    private String reqMethod;

    private String ipAddress;

    @Column(length = 1000)
    private String userAgent;

    @Lob
    private String reqHeader;

    @Lob
    private String reqBody;

    @Lob
    private String resBody;

    private String status;

    private Long duration;

    @Lob
    private String exceptionMessage;

    private Long startTimestamp;

    private Long endTimestamp;

    @CreatedDate
    private LocalDateTime createDateTime;

    @CreatedBy
    private String createBy;
}
