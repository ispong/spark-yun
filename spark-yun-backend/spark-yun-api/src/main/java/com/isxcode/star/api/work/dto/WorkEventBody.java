package com.isxcode.star.api.work.dto;

import com.isxcode.star.api.cluster.dto.ScpFileEngineNodeDto;
import lombok.Data;

@Data
public class WorkEventBody {

    private String script;

    private ScpFileEngineNodeDto scpNodeInfo;

    private String agentHomePath;

    private String currentStatus;

    private String pid;
}
