package com.isxcode.spark.api.ai.req;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class SaveAiChatSessionReq {

    private String id;

    @NotEmpty(message = "智能配置不能为空")
    private String configId;

    @NotEmpty(message = "会话标题不能为空")
    private String title;

    @NotEmpty(message = "消息不能为空")
    private List<AiChatReq.AiChatMessageReq> messages;
}
