package com.isxcode.spark.api.ai.req;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class AiChatReq {

    @NotEmpty(message = "智能配置不能为空")
    private String configId;

    @NotEmpty(message = "消息不能为空")
    private List<AiChatMessageReq> messages;

    @Data
    public static class AiChatMessageReq {

        private String role;

        private String content;
    }
}
