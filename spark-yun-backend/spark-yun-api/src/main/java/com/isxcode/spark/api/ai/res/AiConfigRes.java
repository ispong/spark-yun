package com.isxcode.spark.api.ai.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiConfigRes {

    private String id;

    private String name;

    private String providerType;

    private String baseUrl;

    private String modelName;

    private Double temperature;

    private Integer maxTokens;

    private String status;

    private String remark;

    private String createDateTime;
}
