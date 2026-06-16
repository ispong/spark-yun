package com.isxcode.spark.api.ai.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiPromptRes {

    private String id;

    private String name;

    private String content;

    private String updatedAt;
}
