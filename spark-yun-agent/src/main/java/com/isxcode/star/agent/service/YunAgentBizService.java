package com.isxcode.star.agent.service;

import com.alibaba.fastjson.JSON;
import com.isxcode.star.agent.run.KubernetesAgentService;
import com.isxcode.star.agent.run.StandaloneAgentService;
import com.isxcode.star.agent.run.YarnAgentService;
import com.isxcode.star.api.agent.constants.AgentType;
import com.isxcode.star.api.agent.pojos.req.YagExecuteWorkReq;
import com.isxcode.star.api.agent.pojos.res.*;
import com.isxcode.star.backend.api.base.exceptions.IsxAppException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkLauncher;
import org.springframework.stereotype.Service;

/**
 * 代理服务层.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YunAgentBizService {

	private final KubernetesAgentService kubernetesAgentService;

	private final YarnAgentService yarnAgentService;

	private final StandaloneAgentService standaloneAgentService;

	public ExecuteWorkRes executeWork(YagExecuteWorkReq yagExecuteWorkReq) throws IOException {

		SparkLauncher sparkLauncher;
		String appId;
		switch (yagExecuteWorkReq.getAgentType()) {
			case AgentType.YARN :
				sparkLauncher = yarnAgentService.genSparkLauncher(yagExecuteWorkReq);
				appId = yarnAgentService.executeWork(sparkLauncher);
				break;
			case AgentType.K8S :
				sparkLauncher = kubernetesAgentService.genSparkLauncher(yagExecuteWorkReq);
				appId = kubernetesAgentService.executeWork(sparkLauncher);
				break;
			case AgentType.StandAlone :
				sparkLauncher = standaloneAgentService.genSparkLauncher(yagExecuteWorkReq);
				appId = standaloneAgentService.executeWork(sparkLauncher);
				break;
			default :
				throw new IsxAppException("agent类型不支持");
		}

		return ExecuteWorkRes.builder().appId(appId).build();
	}

	public YagGetStatusRes getStatus(String appId, String agentType, String sparkHomePath) throws IOException {

		String appStatus;
		switch (agentType) {
			case AgentType.YARN :
				appStatus = yarnAgentService.getAppStatus(appId, sparkHomePath);
				break;
			case AgentType.K8S :
				appStatus = kubernetesAgentService.getAppStatus(appId, sparkHomePath);
				break;
			case AgentType.StandAlone :
				appStatus = standaloneAgentService.getAppStatus(appId, sparkHomePath);
				break;
			default :
				throw new IsxAppException("agent类型不支持");
		}

		return YagGetStatusRes.builder().appId(appId).appStatus(appStatus).build();
	}

	public YagGetLogRes getLog(String appId, String agentType, String sparkHomePath) throws IOException {

		String appLog;
		switch (agentType) {
			case AgentType.YARN :
				appLog = yarnAgentService.getAppLog(appId, sparkHomePath);
				break;
			case AgentType.K8S :
				appLog = kubernetesAgentService.getAppLog(appId, sparkHomePath);
				break;
			case AgentType.StandAlone :
				appLog = standaloneAgentService.getAppLog(appId, sparkHomePath);
				break;
			default :
				throw new IsxAppException("agent类型不支持");
		}

		return YagGetLogRes.builder().log(appLog).build();
	}

	public YagGetStdoutLogRes getStdoutLog(String appId, String agentType, String sparkHomePath) throws IOException {

		String appLog;
		switch (agentType) {
			case AgentType.YARN :
				appLog = yarnAgentService.getStdoutLog(appId, sparkHomePath);
				break;
			case AgentType.K8S :
				appLog = kubernetesAgentService.getStdoutLog(appId, sparkHomePath);
				break;
			case AgentType.StandAlone :
				appLog = standaloneAgentService.getStdoutLog(appId, sparkHomePath);
				break;
			default :
				throw new IsxAppException("agent类型不支持");
		}

		// 只截取后10行的日志
		appLog = appLog.replace("End of LogType:stdout", "").replace("LogType:stdout-start", "");
		String[] split = appLog.split("\n");
		List<String> list = Arrays.asList(split);
		if (list.size() > 5) {
			list = list.subList(list.size() - 5, list.size());
		} else {
			list = list.subList(0, list.size());
		}
		StringBuilder stringBuilder = new StringBuilder();
		for (String str : list) {
			stringBuilder.append(str).append("\n");
		}
		return YagGetStdoutLogRes.builder().log(stringBuilder.toString()).build();
	}

	public YagGetDataRes getData(String appId, String agentType, String sparkHomePath) throws IOException {

		String stdoutLog;
		switch (agentType) {
			case AgentType.YARN :
				stdoutLog = yarnAgentService.getAppData(appId, sparkHomePath);
				break;
			case AgentType.K8S :
				stdoutLog = kubernetesAgentService.getAppData(appId, sparkHomePath);
				break;
			case AgentType.StandAlone :
				stdoutLog = standaloneAgentService.getAppData(appId, sparkHomePath);
				break;
			default :
				throw new IsxAppException("agent类型不支持");
		}

		return YagGetDataRes.builder().data(JSON.parseArray(stdoutLog, List.class)).build();
	}

	public void stopJob(String appId, String agentType, String sparkHomePath) throws IOException {

		switch (agentType) {
			case AgentType.YARN :
				yarnAgentService.killApp(appId, sparkHomePath);
				break;
			case AgentType.K8S :
				kubernetesAgentService.killApp(appId, sparkHomePath);
				break;
			case AgentType.StandAlone :
				standaloneAgentService.killApp(appId, sparkHomePath);
				break;
			default :
				throw new IsxAppException("agent类型不支持");
		}
	}
}
