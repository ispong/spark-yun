package com.isxcode.spark.api.agent.req.spark;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CleanLocalScriptReq {

    private String agentHomePath;

    private String workInstanceId;

    private String scriptSuffix;
}
