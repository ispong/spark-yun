package com.isxcode.spark.api.agent.req.spark;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UploadAgentFileReq {

    private String agentHomePath;

    private String directory;

    private String fileName;

    private String contentBase64;
}
