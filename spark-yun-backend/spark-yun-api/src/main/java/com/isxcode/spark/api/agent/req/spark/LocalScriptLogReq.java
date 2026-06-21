package com.isxcode.spark.api.agent.req.spark;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LocalScriptLogReq {

    private String agentHomePath;

    private String workInstanceId;
}
