package com.isxcode.spark.api.auth.res;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.isxcode.spark.backend.api.base.serializer.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PageLoginLogRes {

    private String id;

    private String loginMethod;

    private String accountIdentifier;

    private String userId;

    private String ipAddress;

    private String userAgent;

    private String loginStatus;

    private Boolean registered;

    private String errorMessage;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createDateTime;
}
