package com.isxcode.spark.api.agent.req.spark;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SubmitLocalScriptReq {

    private String agentHomePath;

    private String workInstanceId;

    private String script;

    private String scriptSuffix;

    private String command;
}
