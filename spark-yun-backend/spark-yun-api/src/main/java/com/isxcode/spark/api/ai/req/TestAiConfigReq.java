package com.isxcode.spark.api.ai.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class TestAiConfigReq {

    @NotEmpty(message = "智能配置不能为空")
    private String id;
}
