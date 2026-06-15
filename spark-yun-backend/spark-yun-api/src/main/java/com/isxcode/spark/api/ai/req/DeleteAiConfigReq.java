package com.isxcode.spark.api.ai.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class DeleteAiConfigReq {

    @NotEmpty(message = "配置id不能为空")
    private String id;
}
