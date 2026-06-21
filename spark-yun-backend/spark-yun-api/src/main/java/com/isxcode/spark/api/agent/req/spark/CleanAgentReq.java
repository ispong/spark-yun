package com.isxcode.spark.api.agent.req.spark;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CleanAgentReq {

    private String username;
}
