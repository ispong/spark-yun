package com.isxcode.spark.api.ai.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class SaveAiPromptReq {

    private String id;

    @NotEmpty(message = "提示词名称不能为空")
    private String name;

    @NotEmpty(message = "提示词内容不能为空")
    private String content;
}
