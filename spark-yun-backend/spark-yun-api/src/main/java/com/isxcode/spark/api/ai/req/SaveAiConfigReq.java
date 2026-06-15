package com.isxcode.spark.api.ai.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class SaveAiConfigReq {

    private String id;

    @NotEmpty(message = "配置名称不能为空")
    private String name;

    @NotEmpty(message = "供应商类型不能为空")
    private String providerType;

    private String baseUrl;

    private String apiKey;

    @NotEmpty(message = "模型不能为空")
    private String modelName;

    private Double temperature;

    private Integer maxTokens;

    private String status;

    private String remark;
}
