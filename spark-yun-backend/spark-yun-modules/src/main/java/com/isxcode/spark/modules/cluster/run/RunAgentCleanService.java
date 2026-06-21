package com.isxcode.spark.modules.cluster.run;

import com.isxcode.spark.api.agent.constants.SparkAgentUrl;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.modules.cluster.entity.ClusterNodeEntity;
import com.isxcode.spark.modules.cluster.repository.ClusterNodeRepository;
import com.isxcode.spark.modules.work.run.AgentLinkUtils;
import com.isxcode.spark.api.work.res.AgentLinkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class, noRollbackFor = {IsxAppException.class})
public class RunAgentCleanService {

    private final ClusterNodeRepository clusterNodeRepository;

    private final AgentLinkUtils agentLinkUtils;

    public void run(String clusterNodeId, String tenantId, String userId) {

        // 获取节点信息
        Optional<ClusterNodeEntity> clusterNodeEntityOptional = clusterNodeRepository.findById(clusterNodeId);
        if (!clusterNodeEntityOptional.isPresent()) {
            return;
        }
        ClusterNodeEntity clusterNodeEntity = clusterNodeEntityOptional.get();

        try {
            AgentLinkResponse response = agentLinkUtils.getAgentLinkResponse(clusterNodeEntity,
                SparkAgentUrl.CLEAN_AGENT_URL, com.isxcode.spark.api.agent.req.spark.CleanAgentReq.builder()
                    .username(clusterNodeEntity.getUsername()).build());
            clusterNodeEntity.setAgentLog(response.getLog() == null ? "清理成功" : response.getLog());
            clusterNodeRepository.saveAndFlush(clusterNodeEntity);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            clusterNodeEntity.setAgentLog("清理失败: " + e.getMessage());
            clusterNodeRepository.saveAndFlush(clusterNodeEntity);
            throw new IsxAppException("清理失败");
        }
    }
}
