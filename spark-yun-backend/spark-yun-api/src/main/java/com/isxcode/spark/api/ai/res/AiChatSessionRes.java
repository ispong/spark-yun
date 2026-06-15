package com.isxcode.spark.api.ai.res;

import com.isxcode.spark.api.ai.req.AiChatReq;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiChatSessionRes {

    private String id;

    private String configId;

    private String title;

    private String updatedAt;

    private List<AiChatReq.AiChatMessageReq> messages;
}
