package com.isxcode.spark.modules.cluster.service.biz;

import com.isxcode.spark.common.security.ContextHolder;

import com.isxcode.spark.api.agent.constants.AgentType;
import com.isxcode.spark.api.agent.constants.SparkAgentUrl;
import com.isxcode.spark.api.api.constants.PathConstants;
import com.isxcode.spark.api.cluster.constants.ClusterNodeConnectType;
import com.isxcode.spark.api.cluster.constants.ClusterNodeStatus;
import com.isxcode.spark.api.cluster.constants.ClusterStatus;
import com.isxcode.spark.api.cluster.dto.ScpFileEngineNodeDto;
import com.isxcode.spark.api.cluster.req.*;
import com.isxcode.spark.api.cluster.res.QueryNodeRes;
import com.isxcode.spark.api.cluster.res.GetClusterNodeRes;
import com.isxcode.spark.api.cluster.res.TestAgentRes;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.common.utils.aes.AesUtils;
import com.isxcode.spark.common.utils.ssh.SshUtils;
import com.isxcode.spark.modules.cluster.entity.ClusterEntity;
import com.isxcode.spark.modules.cluster.entity.ClusterNodeEntity;
import com.isxcode.spark.modules.cluster.mapper.ClusterNodeMapper;
import com.isxcode.spark.modules.cluster.repository.ClusterNodeRepository;
import com.isxcode.spark.modules.cluster.repository.ClusterRepository;
import com.isxcode.spark.modules.cluster.run.*;
import com.isxcode.spark.modules.cluster.service.ClusterNodeService;
import com.isxcode.spark.modules.cluster.service.ClusterService;
import com.isxcode.spark.modules.work.run.AgentLinkUtils;

import java.io.IOException;

import com.jcraft.jsch.JSchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class, noRollbackFor = {IsxAppException.class})
@Slf4j
public class ClusterNodeBizService {

    private final ClusterNodeRepository clusterNodeRepository;

    private final ClusterRepository clusterRepository;

    private final ClusterService clusterService;

    private final ClusterNodeMapper engineNodeMapper;

    private final RunAgentCheckService runAgentCheckService;

    private final RunAgentInstallService runAgentInstallService;

    private final RunAgentStopService runAgentStopService;

    private final RunAgentStartService runAgentStartService;

    private final RunAgentRemoveService runAgentRemoveService;

    private final RunAgentCleanService runAgentCleanService;

    private final AesUtils aesUtils;

    private final ClusterNodeService clusterNodeService;

    private final AgentLinkUtils agentLinkUtils;

    public void addClusterNode(AddClusterNodeReq addClusterNodeReq) {

        ClusterEntity cluster = clusterService.getCluster(addClusterNodeReq.getClusterId());

        validateClusterNodeNameUnique(addClusterNodeReq.getClusterId(), addClusterNodeReq.getName(), null);

        ClusterNodeEntity clusterNode = engineNodeMapper.addClusterNodeReqToClusterNodeEntity(addClusterNodeReq);

        String connectType = clusterNodeService.getDefaultConnectType(addClusterNodeReq.getConnectType());
        clusterNode.setConnectType(connectType);

        // 是否安装spark-local组件
        clusterNode.setInstallSparkLocal(Boolean.TRUE.equals(addClusterNodeReq.getInstallSparkLocal()));
        clusterNode.setInstallFlinkLocal(Boolean.TRUE.equals(addClusterNodeReq.getInstallFlinkLocal()));

        if (ClusterNodeConnectType.AGENT_PORT.equals(connectType)) {
            clusterNodeService.validateAgentPortConnectConfig(addClusterNodeReq.getAgentPort());
            clusterNode.setAgentPort(addClusterNodeReq.getAgentPort());
            clusterNode.setPort(null);
            clusterNode.setUsername(null);
            clusterNode.setPasswd(null);
            clusterNode.setAgentHomePath(null);
            clusterNode.setHadoopHomePath(null);
            clusterNode.setSparkHomePath(addClusterNodeReq.getSparkHomePath());
            clusterNode.setFlinkHomePath(addClusterNodeReq.getFlinkHomePath());
            clusterNode.setInstallSparkLocal(false);
            clusterNode.setInstallFlinkLocal(false);
            clusterNode.setStatus(ClusterNodeStatus.UN_INSTALL);
            clusterNodeRepository.save(clusterNode);
            return;
        }

        clusterNodeService.validateSshConnectConfig(addClusterNodeReq.getPort(), addClusterNodeReq.getUsername(),
            addClusterNodeReq.getPasswd());

        clusterNode.setAgentPort(clusterNodeService.getDefaultAgentPort(addClusterNodeReq.getAgentPort()));

        // 设置服务器默认端口号
        clusterNode.setPort(addClusterNodeReq.getPort());

        // 密码对成加密
        clusterNode.setPasswd(aesUtils.encrypt(addClusterNodeReq.getPasswd().trim()));

        // 初始化节点状态，未检测
        clusterNode.setStatus(ClusterNodeStatus.UN_INSTALL);

        // 设置默认代理安装地址
        clusterNode.setAgentHomePath(
            clusterNodeService.getDefaultAgentHomePath(addClusterNodeReq.getUsername().trim(), clusterNode));

        // 如果是默认安装spark,设置默认路径
        if (Boolean.TRUE.equals(addClusterNodeReq.getInstallSparkLocal())
            || !AgentType.StandAlone.equals(cluster.getClusterType())) {
            clusterNode.setSparkHomePath(clusterNode.getAgentHomePath() + "/" + PathConstants.AGENT_PATH_NAME + "/"
                + PathConstants.SPARK_MIN_HOME);
        } else {
            clusterNode.setSparkHomePath(addClusterNodeReq.getSparkHomePath());
        }

        // 如果是默认安装flink,设置默认路径
        if (Boolean.TRUE.equals(addClusterNodeReq.getInstallFlinkLocal())
            || !AgentType.StandAlone.equals(cluster.getClusterType())) {
            clusterNode.setFlinkHomePath(clusterNode.getAgentHomePath() + "/" + PathConstants.AGENT_PATH_NAME + "/"
                + PathConstants.FLINK_MIN_HOME);
        } else {
            clusterNode.setFlinkHomePath(addClusterNodeReq.getFlinkHomePath());
        }

        // 持久化数据
        clusterNodeRepository.save(clusterNode);
    }

    public void updateClusterNode(UpdateClusterNodeReq updateClusterNodeReq) {

        ClusterEntity cluster = clusterService.getCluster(updateClusterNodeReq.getClusterId());

        ClusterNodeEntity clusterNode = clusterNodeService.getClusterNode(updateClusterNodeReq.getId());

        validateClusterNodeNameUnique(updateClusterNodeReq.getClusterId(), updateClusterNodeReq.getName(),
            updateClusterNodeReq.getId());

        // 如果是安装中等状态，需要等待运行结束
        if (ClusterNodeStatus.CHECKING.equals(clusterNode.getStatus())
            || ClusterNodeStatus.INSTALLING.equals(clusterNode.getStatus())
            || ClusterNodeStatus.REMOVING.equals(clusterNode.getStatus())
            || ClusterNodeStatus.STARTING.equals(clusterNode.getStatus())
            || ClusterNodeStatus.STOPPING.equals(clusterNode.getStatus())) {
            throw new IsxAppException("当前状态无法操作，请稍后再试");
        }

        // 转换对象
        clusterNode = engineNodeMapper.updateNodeReqToNodeEntity(updateClusterNodeReq, clusterNode);

        String connectType = clusterNodeService.getDefaultConnectType(updateClusterNodeReq.getConnectType());
        clusterNode.setConnectType(connectType);

        // 是否安装spark-local组件
        clusterNode.setInstallSparkLocal(Boolean.TRUE.equals(updateClusterNodeReq.getInstallSparkLocal()));
        clusterNode.setInstallFlinkLocal(Boolean.TRUE.equals(updateClusterNodeReq.getInstallFlinkLocal()));

        if (ClusterNodeConnectType.AGENT_PORT.equals(connectType)) {
            clusterNodeService.validateAgentPortConnectConfig(updateClusterNodeReq.getAgentPort());
            clusterNode.setAgentPort(updateClusterNodeReq.getAgentPort());
            clusterNode.setPort(null);
            clusterNode.setUsername(null);
            clusterNode.setPasswd(null);
            clusterNode.setAgentHomePath(null);
            clusterNode.setHadoopHomePath(null);
            clusterNode.setSparkHomePath(updateClusterNodeReq.getSparkHomePath());
            clusterNode.setFlinkHomePath(updateClusterNodeReq.getFlinkHomePath());
            clusterNode.setInstallSparkLocal(false);
            clusterNode.setInstallFlinkLocal(false);
            clusterNode.setStatus(ClusterNodeStatus.UN_CHECK);
            clusterNodeRepository.save(clusterNode);

            cluster.setStatus(ClusterStatus.UN_CHECK);
            clusterRepository.save(cluster);
            return;
        }

        clusterNodeService.validateSshConnectConfig(updateClusterNodeReq.getPort(), updateClusterNodeReq.getUsername(),
            updateClusterNodeReq.getPasswd());

        // 设置代理端口号
        clusterNode.setAgentPort(clusterNodeService.getDefaultAgentPort(updateClusterNodeReq.getAgentPort()));

        // 密码对成加密
        clusterNode.setPasswd(aesUtils.encrypt(updateClusterNodeReq.getPasswd().trim()));

        // 保存端口号
        clusterNode.setPort(updateClusterNodeReq.getPort());

        // 设置安装地址
        clusterNode.setAgentHomePath(
            clusterNodeService.getDefaultAgentHomePath(updateClusterNodeReq.getUsername(), clusterNode));

        // 如果是默认安装spark,设置默认路径
        if (Boolean.TRUE.equals(updateClusterNodeReq.getInstallSparkLocal())
            || !AgentType.StandAlone.equals(cluster.getClusterType())) {
            clusterNode.setSparkHomePath(clusterNode.getAgentHomePath() + "/" + PathConstants.AGENT_PATH_NAME + "/"
                + PathConstants.SPARK_MIN_HOME);
        } else {
            clusterNode.setSparkHomePath(updateClusterNodeReq.getSparkHomePath());
        }

        // 如果是默认安装flink,设置默认路径
        if (Boolean.TRUE.equals(updateClusterNodeReq.getInstallFlinkLocal())
            || !AgentType.StandAlone.equals(cluster.getClusterType())) {
            clusterNode.setFlinkHomePath(clusterNode.getAgentHomePath() + "/" + PathConstants.AGENT_PATH_NAME + "/"
                + PathConstants.FLINK_MIN_HOME);
        } else {
            clusterNode.setFlinkHomePath(updateClusterNodeReq.getFlinkHomePath());
        }

        // 初始化节点状态，未检测
        clusterNode.setStatus(ClusterNodeStatus.UN_CHECK);
        clusterNodeRepository.save(clusterNode);

        // 集群状态修改
        cluster.setStatus(ClusterStatus.UN_CHECK);
        clusterRepository.save(cluster);
    }

    private void validateClusterNodeNameUnique(String clusterId, String name, String currentNodeId) {

        clusterNodeRepository.findByClusterIdAndName(clusterId, name).ifPresent(clusterNode -> {
            if (currentNodeId == null || !clusterNode.getId().equals(currentNodeId)) {
                throw new IsxAppException("同一计算集群中节点名称不能重复");
            }
        });
    }

    public Page<QueryNodeRes> pageClusterNode(PageClusterNodeReq enoQueryNodeReq) {

        Page<ClusterNodeEntity> engineNodeEntities = clusterNodeRepository.searchAll(enoQueryNodeReq.getSearchKeyWord(),
            enoQueryNodeReq.getClusterId(), PageRequest.of(enoQueryNodeReq.getPage(), enoQueryNodeReq.getPageSize()));

        return engineNodeEntities.map(engineNodeMapper::nodeEntityToQueryNodeRes);
    }

    public void deleteClusterNode(DeleteClusterNodeReq deleteClusterNodeReq) {

        ClusterNodeEntity clusterNode = clusterNodeRepository.findById(deleteClusterNodeReq.getEngineNodeId())
            .orElseThrow(() -> new IsxAppException("节点已删除"));

        boolean agentPortConnectType = clusterNodeService.isAgentPortConnectType(clusterNode.getConnectType());

        // 如果是安装中等状态，需要等待运行结束
        if (ClusterNodeStatus.CHECKING.equals(clusterNode.getStatus())
            || ClusterNodeStatus.INSTALLING.equals(clusterNode.getStatus())
            || ClusterNodeStatus.REMOVING.equals(clusterNode.getStatus())
            || ClusterNodeStatus.STARTING.equals(clusterNode.getStatus())
            || ClusterNodeStatus.STOPPING.equals(clusterNode.getStatus())
            || (ClusterNodeStatus.RUNNING.equals(clusterNode.getStatus()) && !agentPortConnectType)) {
            throw new IsxAppException("请卸载节点后删除");
        }

        clusterNodeRepository.deleteById(deleteClusterNodeReq.getEngineNodeId());
    }

    public void checkAgent(CheckAgentReq checkAgentReq) {

        // 获取节点信息
        ClusterNodeEntity engineNode = clusterNodeService.getClusterNode(checkAgentReq.getEngineNodeId());

        // 如果是安装中等状态，需要等待运行结束
        if (ClusterNodeStatus.CHECKING.equals(engineNode.getStatus())
            || ClusterNodeStatus.INSTALLING.equals(engineNode.getStatus())
            || ClusterNodeStatus.REMOVING.equals(engineNode.getStatus())
            || ClusterNodeStatus.STARTING.equals(engineNode.getStatus())
            || ClusterNodeStatus.STOPPING.equals(engineNode.getStatus())) {
            throw new IsxAppException("当前状态无法操作，请稍后再试");
        }

        ScpFileEngineNodeDto scpFileEngineNodeDto = null;
        if (!clusterNodeService.isAgentPortConnectType(engineNode.getConnectType())) {
            // 转换请求节点检测对象
            scpFileEngineNodeDto = engineNodeMapper.engineNodeEntityToScpFileEngineNodeDto(engineNode);
            scpFileEngineNodeDto.setPasswd(aesUtils.decrypt(scpFileEngineNodeDto.getPasswd()));
        }

        // 修改状态
        engineNode.setStatus(ClusterNodeStatus.CHECKING);
        engineNode.setAgentLog("检测中");

        // 持久化
        clusterNodeRepository.saveAndFlush(engineNode);

        // 异步调用
        runAgentCheckService.run(checkAgentReq.getEngineNodeId(), scpFileEngineNodeDto, ContextHolder.getTenantId(),
            ContextHolder.getUserId());
    }

    public TestAgentRes testAgent(TestAgentReq testAgentReq) {

        if (clusterNodeService.isAgentPortConnectType(testAgentReq.getConnectType())) {
            if (Strings.isEmpty(testAgentReq.getAgentPort())) {
                return TestAgentRes.builder().status("FAIL").log("请输入服务端口号").build();
            }
            ClusterNodeEntity agentNode =
                ClusterNodeEntity.builder().host(testAgentReq.getHost()).agentPort(testAgentReq.getAgentPort()).build();
            try {
                agentLinkUtils.getAgentLinkResponse(agentNode, SparkAgentUrl.HEART_CHECK_URL, null);
                return TestAgentRes.builder().status("SUCCESS").log("连接成功").build();
            } catch (Exception e) {
                return TestAgentRes.builder().status("FAIL").log(e.getMessage()).build();
            }
        }

        if (Strings.isEmpty(testAgentReq.getPort()) || Strings.isEmpty(testAgentReq.getUsername())
            || Strings.isEmpty(testAgentReq.getPasswd())) {
            return TestAgentRes.builder().status("FAIL").log("请将SSH参数填写完整").build();
        }
        ScpFileEngineNodeDto scpFileEngineNodeDto = ScpFileEngineNodeDto.builder().host(testAgentReq.getHost())
            .port(testAgentReq.getPort()).passwd(testAgentReq.getPasswd()).username(testAgentReq.getUsername()).build();
        String testAgent = "echo 'hello'";
        try {
            String testBack = SshUtils.executeCommand(scpFileEngineNodeDto, testAgent, false);
            if ("hello\n".equals(testBack)) {
                return TestAgentRes.builder().status("SUCCESS").log("链接成功").build();
            } else {
                return TestAgentRes.builder().status("FAIL").log(testBack).build();
            }
        } catch (JSchException | InterruptedException | IOException e) {
            return TestAgentRes.builder().status("FAIL").log(e.getMessage()).build();
        }
    }

    /**
     * 安装节点.
     */
    public void installAgent(InstallAgentReq installAgentReq) {

        ClusterNodeEntity clusterNode = clusterNodeService.getClusterNode(installAgentReq.getEngineNodeId());

        ClusterEntity cluster = clusterService.getCluster(clusterNode.getClusterId());

        if (clusterNodeService.isAgentPortConnectType(clusterNode.getConnectType())) {
            throw new IsxAppException("端口连接方式不支持安装节点");
        }

        // 如果是安装中等状态，需要等待运行结束
        if (ClusterNodeStatus.CHECKING.equals(clusterNode.getStatus())
            || ClusterNodeStatus.INSTALLING.equals(clusterNode.getStatus())
            || ClusterNodeStatus.REMOVING.equals(clusterNode.getStatus())
            || ClusterNodeStatus.STARTING.equals(clusterNode.getStatus())
            || ClusterNodeStatus.STOPPING.equals(clusterNode.getStatus())
            || ClusterNodeStatus.RUNNING.equals(clusterNode.getStatus())) {
            throw new IsxAppException("当前状态无法操作，请稍后再试");
        }

        // 将节点信息转成工具类识别对象
        ScpFileEngineNodeDto scpFileEngineNodeDto =
            engineNodeMapper.engineNodeEntityToScpFileEngineNodeDto(clusterNode);
        scpFileEngineNodeDto.setPasswd(aesUtils.decrypt(scpFileEngineNodeDto.getPasswd()));

        // 修改状态
        clusterNode.setStatus(ClusterNodeStatus.INSTALLING);
        clusterNode.setAgentLog("激活中");

        // 持久化
        clusterNodeRepository.saveAndFlush(clusterNode);

        // 异步调用
        runAgentInstallService.run(installAgentReq.getEngineNodeId(), cluster.getClusterType(), scpFileEngineNodeDto,
            ContextHolder.getTenantId(), ContextHolder.getUserId());
    }

    public void removeAgent(RemoveAgentReq removeAgentReq) {

        // 获取节点信息
        ClusterNodeEntity engineNode = clusterNodeService.getClusterNode(removeAgentReq.getEngineNodeId());

        if (clusterNodeService.isAgentPortConnectType(engineNode.getConnectType())) {
            throw new IsxAppException("端口连接方式不支持卸载节点");
        }

        // 如果是安装中等状态，需要等待运行结束
        if (ClusterNodeStatus.CHECKING.equals(engineNode.getStatus())
            || ClusterNodeStatus.INSTALLING.equals(engineNode.getStatus())
            || ClusterNodeStatus.REMOVING.equals(engineNode.getStatus())) {
            throw new IsxAppException("当前状态无法操作，请稍后再试");
        }

        // 将节点信息转成工具类识别对象
        ScpFileEngineNodeDto scpFileEngineNodeDto = engineNodeMapper.engineNodeEntityToScpFileEngineNodeDto(engineNode);
        scpFileEngineNodeDto.setPasswd(aesUtils.decrypt(scpFileEngineNodeDto.getPasswd()));

        // 修改状态
        engineNode.setStatus(ClusterNodeStatus.REMOVING);
        engineNode.setAgentLog("卸载中");

        // 持久化
        clusterNodeRepository.saveAndFlush(engineNode);

        // 异步调用
        runAgentRemoveService.run(removeAgentReq.getEngineNodeId(), scpFileEngineNodeDto, ContextHolder.getTenantId(),
            ContextHolder.getUserId());
    }

    public void cleanAgent(CleanAgentReq cleanAgentReq) {

        // 获取节点信息
        ClusterNodeEntity engineNode = clusterNodeService.getClusterNode(cleanAgentReq.getEngineNodeId());

        // 如果是安装中等状态，需要等待运行结束
        if (ClusterNodeStatus.CHECKING.equals(engineNode.getStatus())
            || ClusterNodeStatus.INSTALLING.equals(engineNode.getStatus())
            || ClusterNodeStatus.REMOVING.equals(engineNode.getStatus())
            || ClusterNodeStatus.STARTING.equals(engineNode.getStatus())
            || ClusterNodeStatus.STOPPING.equals(engineNode.getStatus())) {
            throw new IsxAppException("当前状态无法操作，请稍后再试");
        }

        // 同步调用
        runAgentCleanService.run(cleanAgentReq.getEngineNodeId(), ContextHolder.getTenantId(), ContextHolder.getUserId());
    }

    /**
     * 停止节点.
     */
    public void stopAgent(StopAgentReq stopAgentReq) {

        // 获取节点信息
        ClusterNodeEntity engineNode = clusterNodeService.getClusterNode(stopAgentReq.getEngineNodeId());

        if (clusterNodeService.isAgentPortConnectType(engineNode.getConnectType())) {
            throw new IsxAppException("端口连接方式不支持停止节点");
        }

        // 如果是安装中等状态，需要等待运行结束
        if (ClusterNodeStatus.CHECKING.equals(engineNode.getStatus())
            || ClusterNodeStatus.INSTALLING.equals(engineNode.getStatus())
            || ClusterNodeStatus.REMOVING.equals(engineNode.getStatus())
            || ClusterNodeStatus.STARTING.equals(engineNode.getStatus())
            || ClusterNodeStatus.STOPPING.equals(engineNode.getStatus())) {
            throw new IsxAppException("当前状态无法操作，请稍后再试");
        }

        // 将节点信息转成工具类识别对象
        ScpFileEngineNodeDto scpFileEngineNodeDto = engineNodeMapper.engineNodeEntityToScpFileEngineNodeDto(engineNode);
        scpFileEngineNodeDto.setPasswd(aesUtils.decrypt(scpFileEngineNodeDto.getPasswd()));

        // 修改状态
        engineNode.setStatus(ClusterNodeStatus.STOPPING);
        engineNode.setAgentLog("停止中");

        // 持久化
        clusterNodeRepository.saveAndFlush(engineNode);

        // 异步调用
        runAgentStopService.run(stopAgentReq.getEngineNodeId(), scpFileEngineNodeDto, ContextHolder.getTenantId(),
            ContextHolder.getUserId());
    }

    /**
     * 激活中.
     */
    public void startAgent(StartAgentReq startAgentReq) {

        // 获取节点信息
        ClusterNodeEntity engineNode = clusterNodeService.getClusterNode(startAgentReq.getEngineNodeId());

        if (clusterNodeService.isAgentPortConnectType(engineNode.getConnectType())) {
            throw new IsxAppException("端口连接方式不支持激活节点");
        }

        // 如果是安装中等状态，需要等待运行结束
        if (ClusterNodeStatus.CHECKING.equals(engineNode.getStatus())
            || ClusterNodeStatus.INSTALLING.equals(engineNode.getStatus())
            || ClusterNodeStatus.REMOVING.equals(engineNode.getStatus())
            || ClusterNodeStatus.STARTING.equals(engineNode.getStatus())
            || ClusterNodeStatus.STOPPING.equals(engineNode.getStatus())) {
            throw new IsxAppException("当前状态无法操作，请稍后再试");
        }

        // 将节点信息转成工具类识别对象
        ScpFileEngineNodeDto scpFileEngineNodeDto = engineNodeMapper.engineNodeEntityToScpFileEngineNodeDto(engineNode);
        scpFileEngineNodeDto.setPasswd(aesUtils.decrypt(scpFileEngineNodeDto.getPasswd()));

        // 修改状态
        engineNode.setStatus(ClusterNodeStatus.STARTING);
        engineNode.setAgentLog("启动中");

        // 持久化
        clusterNodeRepository.saveAndFlush(engineNode);

        // 异步调用
        runAgentStartService.run(startAgentReq.getEngineNodeId(), scpFileEngineNodeDto, ContextHolder.getTenantId(),
            ContextHolder.getUserId());
    }

    public GetClusterNodeRes getClusterNode(GetClusterNodeReq getClusterNodeReq) {

        ClusterNodeEntity clusterNode = clusterNodeService.getClusterNode(getClusterNodeReq.getClusterNodeId());
        return engineNodeMapper.clusterNodeEntityToGetClusterNodeRes(clusterNode);
    }
}
