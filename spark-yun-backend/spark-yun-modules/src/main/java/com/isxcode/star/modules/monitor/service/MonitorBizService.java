package com.isxcode.star.modules.monitor.service;

import com.alibaba.fastjson.JSON;
import com.isxcode.star.api.cluster.pojos.dto.ScpFileEngineNodeDto;
import com.isxcode.star.api.main.properties.SparkYunProperties;
import com.isxcode.star.api.monitor.constants.MonitorStatus;
import com.isxcode.star.api.monitor.dto.MonitorLineDto;
import com.isxcode.star.api.monitor.dto.NodeMonitorInfo;
import com.isxcode.star.api.monitor.dto.SystemMonitorDto;
import com.isxcode.star.api.monitor.req.GetInstanceMonitorReq;
import com.isxcode.star.api.monitor.req.QueryInstancesReq;
import com.isxcode.star.api.monitor.res.GetClusterMonitorRes;
import com.isxcode.star.api.monitor.res.GetInstanceMonitorRes;
import com.isxcode.star.api.monitor.res.GetSystemMonitorRes;
import com.isxcode.star.api.monitor.res.QueryInstancesRes;
import com.isxcode.star.common.utils.AesUtils;
import com.isxcode.star.modules.cluster.entity.ClusterNodeEntity;
import com.isxcode.star.modules.cluster.mapper.ClusterNodeMapper;
import com.isxcode.star.modules.cluster.repository.ClusterNodeRepository;
import com.isxcode.star.modules.monitor.entity.MonitorEntity;
import com.isxcode.star.modules.monitor.mapper.MonitorMapper;
import com.isxcode.star.modules.monitor.repository.MonitorRepository;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.isxcode.star.common.config.CommonConfig.JPA_TENANT_MODE;
import static com.isxcode.star.common.utils.ssh.SshUtils.executeCommand;
import static com.isxcode.star.common.utils.ssh.SshUtils.scpFile;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MonitorBizService {

	private final ClusterNodeRepository clusterNodeRepository;

	private final SparkYunProperties sparkYunProperties;

	private final ClusterNodeMapper clusterNodeMapper;

	private final MonitorMapper monitorMapper;

	private final AesUtils aesUtils;

	private final MonitorRepository monitorRepository;

	public GetSystemMonitorRes getSystemMonitor() {

		// 查询集群信息
		SystemMonitorDto clusterMonitor = new SystemMonitorDto();

		// 查询数据源
		SystemMonitorDto datasourceMonitor = new SystemMonitorDto();

		// 查询作业信息
		SystemMonitorDto workMonitor = new SystemMonitorDto();

		// 查询接口信息
		SystemMonitorDto apiMonitor = new SystemMonitorDto();
		apiMonitor.setTotal(10);
		apiMonitor.setActiveNum(7);

		return GetSystemMonitorRes.builder().apiMonitor(apiMonitor).workMonitor(workMonitor)
				.clusterMonitor(clusterMonitor).datasourceMonitor(datasourceMonitor).build();
	}

	public GetClusterMonitorRes getClusterMonitor() {

		List<MonitorLineDto> monitorLineDtos = monitorRepository.queryMonitorLine();

		return GetClusterMonitorRes.builder().line(monitorLineDtos).build();
	}

	public GetInstanceMonitorRes getInstanceMonitor(GetInstanceMonitorReq getInstanceMonitorReq) {

		return null;
	}

	public QueryInstancesRes queryInstances(QueryInstancesReq queryInstancesReq) {

		return null;
	}

	@Scheduled(cron = "0 * * * * ?")
	public void scheduleGetNodeMonitor() {

		LocalDateTime now = LocalDateTime.now();

		// 获取所有的节点
		JPA_TENANT_MODE.set(false);
		List<ClusterNodeEntity> allNode = clusterNodeRepository.findAll();

		allNode.forEach(e -> {
			CompletableFuture.supplyAsync(() -> {

				// 封装ScpFileEngineNodeDto对象
				ScpFileEngineNodeDto scpFileEngineNodeDto = clusterNodeMapper.engineNodeEntityToScpFileEngineNodeDto(e);
				scpFileEngineNodeDto.setPasswd(aesUtils.decrypt(scpFileEngineNodeDto.getPasswd()));

				// 每个节点都抽取一次
				try {
					NodeMonitorInfo nodeMonitor = getNodeMonitor(scpFileEngineNodeDto);
					nodeMonitor.setClusterNodeId(e.getId());
					nodeMonitor.setClusterId(e.getClusterId());
					nodeMonitor.setTenantId(e.getTenantId());
					nodeMonitor.setCreateDateTime(now);
					return nodeMonitor;
				} catch (Exception ex) {
					return NodeMonitorInfo.builder().clusterNodeId(e.getId()).clusterNodeId(e.getClusterId())
							.status(MonitorStatus.FAIL).log(ex.getMessage()).tenantId(e.getTenantId())
							.createDateTime(now).build();
				}
			}).whenComplete((result, throwable) -> {
				// 持久化到数据库
				MonitorEntity monitorEntity = monitorMapper.nodeMonitorInfoToMonitorEntity(result);
				monitorRepository.save(monitorEntity);
			});
		});
	}

	public NodeMonitorInfo getNodeMonitor(ScpFileEngineNodeDto scpFileEngineNodeDto)
			throws JSchException, IOException, InterruptedException, SftpException {

		// 拷贝检测脚本
		scpFile(scpFileEngineNodeDto, "classpath:bash/node-monitor.sh",
				sparkYunProperties.getTmpDir() + File.separator + "node-monitor.sh");

		// 运行安装脚本
		String getMonitorCommand = "bash " + sparkYunProperties.getTmpDir() + File.separator + "node-monitor.sh";

		// 获取返回结果
		String executeLog = executeCommand(scpFileEngineNodeDto, getMonitorCommand, false);

		// 获取节点信息
		return JSON.parseObject(executeLog, NodeMonitorInfo.class);
	}
}
