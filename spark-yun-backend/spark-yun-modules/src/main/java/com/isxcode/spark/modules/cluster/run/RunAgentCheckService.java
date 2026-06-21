package com.isxcode.spark.modules.cluster.run;

import com.isxcode.spark.common.security.ContextHolder;
import static com.isxcode.spark.common.utils.ssh.SshUtils.executeCommand;
import static com.isxcode.spark.common.utils.ssh.SshUtils.scpFile;

import com.alibaba.fastjson.JSON;
import com.isxcode.spark.api.agent.constants.SparkAgentUrl;
import com.isxcode.spark.api.cluster.constants.ClusterNodeConnectType;
import com.isxcode.spark.api.cluster.constants.ClusterNodeStatus;
import com.isxcode.spark.api.cluster.constants.ClusterStatus;
import com.isxcode.spark.api.cluster.dto.AgentInfo;
import com.isxcode.spark.api.cluster.dto.ScpFileEngineNodeDto;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.modules.cluster.entity.ClusterNodeEntity;
import com.isxcode.spark.modules.cluster.repository.ClusterNodeRepository;
import com.isxcode.spark.modules.cluster.repository.ClusterRepository;
import com.isxcode.spark.modules.work.run.AgentLinkUtils;
import com.jcraft.jsch.JSchException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class, noRollbackFor = {IsxAppException.class})
public class RunAgentCheckService {

    private final ClusterNodeRepository clusterNodeRepository;

    private final ClusterRepository clusterRepository;

    private final AgentLinkUtils agentLinkUtils;

    @Async("sparkYunWorkThreadPool")
    public void run(String clusterNodeId, ScpFileEngineNodeDto scpFileEngineNodeDto, String tenantId, String userId) {

        ContextHolder.setUserId(userId);
        ContextHolder.setTenantId(tenantId);

        // 获取节点信息
        Optional<ClusterNodeEntity> clusterNodeEntityOptional = clusterNodeRepository.findById(clusterNodeId);
        if (!clusterNodeEntityOptional.isPresent()) {
            return;
        }
        ClusterNodeEntity clusterNodeEntity = clusterNodeEntityOptional.get();

        try {
            checkAgent(scpFileEngineNodeDto, clusterNodeEntity);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            clusterNodeEntity.setCheckDateTime(LocalDateTime.now());
            clusterNodeEntity.setAgentLog(e.getMessage());
            clusterNodeEntity.setStatus(ClusterNodeStatus.CHECK_ERROR);
            clusterNodeRepository.saveAndFlush(clusterNodeEntity);
        }
    }

    public void checkAgent(ScpFileEngineNodeDto scpFileEngineNodeDto, ClusterNodeEntity engineNode)
        throws JSchException, IOException, InterruptedException {

        if (ClusterNodeConnectType.AGENT_PORT.equals(engineNode.getConnectType())) {
            checkAgentPort(engineNode);
            return;
        }

        // 运行检测命令
        String checkCommand = buildCheckCommand(engineNode.getAgentHomePath());

        log.debug("执行远程命令:{}", checkCommand);

        // 获取返回结果
        String executeLog = executeCommand(scpFileEngineNodeDto, checkCommand, false);

        log.debug("远程返回值:{}", executeLog);
        AgentInfo agentCheckInfo = JSON.parseObject(executeLog, AgentInfo.class);

        if (agentCheckInfo == null) {
            return;
        }

        // 保存服务器信息
        engineNode.setAllMemory(
            Double.parseDouble(Strings.isEmpty(agentCheckInfo.getAllMemory()) ? "0.0" : agentCheckInfo.getAllMemory()));
        engineNode.setUsedMemory(Double
            .parseDouble(Strings.isEmpty(agentCheckInfo.getUsedMemory()) ? "0.0" : agentCheckInfo.getUsedMemory()));
        engineNode.setAllStorage(Double
            .parseDouble(Strings.isEmpty(agentCheckInfo.getAllStorage()) ? "0.0" : agentCheckInfo.getAllStorage()));
        engineNode.setUsedStorage(Double
            .parseDouble(Strings.isEmpty(agentCheckInfo.getUsedStorage()) ? "0.0" : agentCheckInfo.getUsedStorage()));
        engineNode.setCpuPercent(Double
            .parseDouble(Strings.isEmpty(agentCheckInfo.getCpuPercent()) ? "0.0" : agentCheckInfo.getCpuPercent()));

        // 修改状态
        engineNode.setStatus(agentCheckInfo.getStatus());
        engineNode.setAgentLog(agentCheckInfo.getLog());
        engineNode.setCheckDateTime(LocalDateTime.now());
        clusterNodeRepository.saveAndFlush(engineNode);

        // 如果状态是成功的话,将集群改为启用
        if (ClusterNodeStatus.RUNNING.equals(agentCheckInfo.getStatus())) {
            clusterRepository.findById(engineNode.getClusterId()).ifPresent(clusterEntity -> {
                clusterEntity.setStatus(ClusterStatus.ACTIVE);
                clusterRepository.saveAndFlush(clusterEntity);
            });
        }
    }

    private String buildCheckCommand(String agentHomePath) {

        return "bash -lc " + shellQuote("home_path=" + shellQuote(agentHomePath) + ";"
            + "agent_path=\"${home_path}/zhiqingyun-agent\";"
            + "if [ -e \"${agent_path}/README.md\" ]; then "
            + "if [ -e \"${agent_path}/zhiqingyun-agent.pid\" ]; then "
            + "pid=$(cat \"${agent_path}/zhiqingyun-agent.pid\"); "
            + "if ps -p \"$pid\" > /dev/null 2>&1; then CHECK_STATUS=\"RUNNING\"; else CHECK_STATUS=\"STOP\"; fi; "
            + "else CHECK_STATUS=\"STOP\"; fi; "
            + "else CHECK_STATUS=\"UN_INSTALL\"; fi;"
            + "ALL_MEMORY=$(free | grep Mem: | awk '{printf \"%.1f\", $2/1024/1024}');"
            + "USED_MEMORY=$(free | grep Mem: | awk '{printf \"%.1f\", $3/1024/1024}');"
            + "ALL_STORAGE=$(lsblk -b | grep disk | awk '{total += $4} END {printf \"%.1f\", total/1024/1024/1024}');"
            + "USED_STORAGE=$(df -B 1 -T | egrep 'ext4|xfs|btrfs' "
            + "| awk '{total += $4} END {printf \"%.1f\",total/1024/1024/1024}');"
            + "CPU_PERCENT=$(top -bn 1 | grep \"Cpu(s)\" | awk -F',' '{print 100 - $4}' | awk '{print $1}');"
            + "printf '{\"status\":\"%s\",\"log\":\"检测完成\",\"allMemory\":\"%s\",\"usedMemory\":\"%s\","
            + "\"allStorage\":\"%s\",\"usedStorage\":\"%s\",\"cpuPercent\":\"%s\"}\\n' \"$CHECK_STATUS\" "
            + "\"$ALL_MEMORY\" \"$USED_MEMORY\" \"$ALL_STORAGE\" \"$USED_STORAGE\" \"$CPU_PERCENT\";");
    }

    private String shellQuote(String value) {

        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private void checkAgentPort(ClusterNodeEntity engineNode) {

        try {
            agentLinkUtils.getAgentLinkResponse(engineNode, SparkAgentUrl.HEART_CHECK_URL, null);
            engineNode.setAllMemory(0.0);
            engineNode.setUsedMemory(0.0);
            engineNode.setAllStorage(0.0);
            engineNode.setUsedStorage(0.0);
            engineNode.setCpuPercent(0.0);
            engineNode.setStatus(ClusterNodeStatus.RUNNING);
            engineNode.setAgentLog("检测完成");
            engineNode.setCheckDateTime(LocalDateTime.now());
            clusterNodeRepository.saveAndFlush(engineNode);

            clusterRepository.findById(engineNode.getClusterId()).ifPresent(clusterEntity -> {
                clusterEntity.setStatus(ClusterStatus.ACTIVE);
                clusterRepository.saveAndFlush(clusterEntity);
            });
        } catch (Exception e) {
            engineNode.setCheckDateTime(LocalDateTime.now());
            engineNode.setAgentLog(e.getMessage());
            engineNode.setStatus(ClusterNodeStatus.CHECK_ERROR);
            clusterNodeRepository.saveAndFlush(engineNode);
        }
    }
}
