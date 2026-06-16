package com.isxcode.spark.api.ai.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiChatFileRes {

    private String name;

    private String contentType;

    private Long size;

    private String content;
}
