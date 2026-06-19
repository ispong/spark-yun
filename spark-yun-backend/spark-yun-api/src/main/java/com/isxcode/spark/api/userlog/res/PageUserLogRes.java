package com.isxcode.spark.api.userlog.res;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.isxcode.spark.backend.api.base.serializer.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PageUserLogRes {

    private String id;

    private String userId;

    private String account;

    private String tenantId;

    private String tenantName;

    private String logType;

    private String moduleCode;

    private String moduleName;

    private String apiName;

    private String reqPath;

    private String reqMethod;

    private String reqBody;

    private String resBody;

    private String status;

    private Long duration;

    private String ipAddress;

    private String userAgent;

    private String exceptionMessage;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createDateTime;
}
