package com.isxcode.star.modules.work.run;

import com.alibaba.fastjson.JSON;
import com.isxcode.star.api.agent.pojos.req.ExecuteContainerSqlReq;
import com.isxcode.star.api.agent.pojos.res.ContainerGetDataRes;
import com.isxcode.star.api.cluster.constants.ClusterNodeStatus;
import com.isxcode.star.api.container.constants.ContainerStatus;
import com.isxcode.star.api.work.constants.WorkLog;
import com.isxcode.star.api.work.exceptions.WorkRunException;
import com.isxcode.star.backend.api.base.exceptions.IsxAppException;
import com.isxcode.star.backend.api.base.pojos.BaseResponse;
import com.isxcode.star.backend.api.base.properties.IsxAppProperties;
import com.isxcode.star.common.utils.http.HttpUtils;
import com.isxcode.star.modules.cluster.entity.ClusterNodeEntity;
import com.isxcode.star.modules.cluster.repository.ClusterNodeRepository;
import com.isxcode.star.modules.container.entity.ContainerEntity;
import com.isxcode.star.modules.container.repository.ContainerRepository;
import com.isxcode.star.modules.work.entity.WorkInstanceEntity;
import com.isxcode.star.modules.work.repository.WorkInstanceRepository;
import com.isxcode.star.modules.workflow.repository.WorkflowInstanceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
public class SparkContainerSqlExecutor extends WorkExecutor {

  private final ContainerRepository containerRepository;

  private final ClusterNodeRepository clusterNodeRepository;

  private final IsxAppProperties isxAppProperties;

  private final WorkInstanceRepository workInstanceRepository;

  public SparkContainerSqlExecutor(WorkInstanceRepository workInstanceRepository, WorkflowInstanceRepository workflowInstanceRepository, ContainerRepository containerRepository, ClusterNodeRepository clusterNodeRepository, IsxAppProperties isxAppProperties, WorkInstanceRepository workInstanceRepository1) {
    super(workInstanceRepository, workflowInstanceRepository);
    this.containerRepository = containerRepository;
    this.clusterNodeRepository = clusterNodeRepository;
      this.isxAppProperties = isxAppProperties;
      this.workInstanceRepository = workInstanceRepository1;
  }

  public void execute(WorkRunContext workRunContext, WorkInstanceEntity workInstance) {

    // 获取日志构造器
    StringBuilder logBuilder = workRunContext.getLogBuilder();

    // 检测数据源是否配置
    logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("开始检测运行环境 \n");
    if (Strings.isEmpty(workRunContext.getContainerId())) {
      throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "检测运行环境失败: 未配置有效容器  \n");
    }

    // 检查数据源是否存在
    Optional<ContainerEntity> containerEntityOptional = containerRepository.findById(workRunContext.getContainerId());
    if (!containerEntityOptional.isPresent()) {
      throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "检测运行环境失败: 有效容器不存在  \n");
    }
    if (!ContainerStatus.RUNNING.equals(containerEntityOptional.get().getStatus())) {
      throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "检测运行环境失败: 容器未在运行中状态  \n");
    }

    // 容器检查通过
    logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("检测运行环境完成  \n");
    workInstance = updateInstance(workInstance, logBuilder);

    // 检查脚本是否为空
    if (Strings.isEmpty(workRunContext.getScript())) {
      throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "Sql内容为空 \n");
    }

    // 脚本检查通过
    logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("开始执行作业 \n");
    workInstance = updateInstance(workInstance, logBuilder);


    // 记录结束执行时间
    logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("查询SQL执行成功  \n");
    workInstance = updateInstance(workInstance, logBuilder);


    // 调用代理的接口，获取数据
    try {

      // 获取集群节点
      List<ClusterNodeEntity> allEngineNodes = clusterNodeRepository.findAllByClusterIdAndStatus(containerEntityOptional.get().getClusterId(), ClusterNodeStatus.RUNNING);
      if (allEngineNodes.isEmpty()) {
        throw new IsxAppException("集群不存在可用节点");
      }

      // 节点选择随机数
      ClusterNodeEntity engineNode = allEngineNodes.get(new Random().nextInt(allEngineNodes.size()));

      // 再次调用容器的check接口，确认容器是否成功启动
//		ContainerCheckReq containerCheckReq = ContainerCheckReq.builder().port(String.valueOf(container.getPort())).build();
      ExecuteContainerSqlReq executeContainerSqlReq = ExecuteContainerSqlReq.builder().port("30178").sql(workRunContext.getScript()).build();
      BaseResponse<?> baseResponse;
      try {
        baseResponse = HttpUtils.doPost(
          genHttpUrl(engineNode.getHost(), engineNode.getAgentPort(), "/yag/executeContainerSql"),
          executeContainerSqlReq, BaseResponse.class);
      } catch (IOException e) {
        throw new WorkRunException(e.getMessage());
      }

      ContainerGetDataRes containerGetDataRes = JSON.parseObject(JSON.toJSONString(baseResponse), ContainerGetDataRes.class);
      if (!"200".equals(containerGetDataRes.getCode())) {
        throw new WorkRunException("运行异常");
      }

      // 讲data转为json存到实例中
      logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("数据保存成功  \n");
      workInstance.setSubmitLog(logBuilder.toString());
      workInstance.setResultData(JSON.toJSONString(containerGetDataRes.getData()));
      workInstanceRepository.saveAndFlush(workInstance);
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + e.getMessage() + "\n");
    }
  }

  @Override
  protected void abort(WorkInstanceEntity workInstance) {

    Thread thread = WORK_THREAD.get(workInstance.getId());
    thread.interrupt();
  }

  public String genHttpUrl(String host, String port, String path) {

        String httpProtocol = isxAppProperties.isUseSsl() ? "https://" : "http://";
        String httpHost = isxAppProperties.isUsePort() ? host + ":" + port : host;

        return httpProtocol + httpHost + path;
    }
}
