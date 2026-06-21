package com.isxcode.spark.modules.work.run;

import com.isxcode.spark.api.agent.constants.SparkAgentUrl;
import com.isxcode.spark.api.agent.req.spark.UploadAgentFileReq;
import com.isxcode.spark.api.api.constants.PathConstants;
import com.isxcode.spark.modules.cluster.entity.ClusterNodeEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public final class AgentFileUploadUtils {

    private AgentFileUploadUtils() {

    }

    public static void uploadFile(AgentLinkUtils agentLinkUtils, ClusterNodeEntity agentNode, String sourcePath,
        String directory, String fileName) throws IOException {

        String contentBase64 = Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of(sourcePath)));
        agentLinkUtils.getAgentLinkResponse(agentNode, SparkAgentUrl.UPLOAD_AGENT_FILE_URL,
            UploadAgentFileReq.builder().agentHomePath(agentNode.getAgentHomePath() + "/" + PathConstants.AGENT_PATH_NAME)
                .directory(directory).fileName(fileName).contentBase64(contentBase64).build());
    }
}
