package com.isxcode.spark.api.auth.res;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.isxcode.spark.backend.api.base.serializer.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PageLoginCodeRecordRes {

    private String id;

    private String channel;

    private String receiver;

    private String scene;

    private String sendStatus;

    private String verifyStatus;

    private Boolean registered;

    private Boolean autoTenantCreated;

    private String errorMessage;

    private String providerMessage;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createDateTime;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime verifyDateTime;
}
