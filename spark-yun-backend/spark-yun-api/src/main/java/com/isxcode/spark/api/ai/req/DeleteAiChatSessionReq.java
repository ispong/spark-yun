package com.isxcode.spark.api.ai.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class DeleteAiChatSessionReq {

    @NotEmpty(message = "会话id不能为空")
    private String id;
}
