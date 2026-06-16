package com.isxcode.spark.api.ai.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class DeleteAiPromptReq {

    @NotEmpty(message = "提示词id不能为空")
    private String id;
}
