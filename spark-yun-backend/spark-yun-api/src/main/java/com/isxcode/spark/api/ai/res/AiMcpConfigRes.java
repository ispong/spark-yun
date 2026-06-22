package com.isxcode.spark.api.ai.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiMcpConfigRes {

    private String serverName;

    private String transport;

    private String url;

    private String configJson;
}
