package com.isxcode.spark.agent.service;

import com.alibaba.fastjson.JSON;
import com.isxcode.spark.agent.run.spark.SparkAgentFactory;
import com.isxcode.spark.agent.run.spark.SparkAgentService;
import com.isxcode.spark.api.agent.req.spark.*;
import com.isxcode.spark.api.agent.res.spark.*;
import com.isxcode.spark.api.monitor.constants.MonitorStatus;
import com.isxcode.spark.api.monitor.dto.NodeMonitorInfo;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SparkAgentBizService {

    private final SparkAgentFactory agentFactory;

    public SubmitWorkRes submitWork(SubmitWorkReq submitWorkReq) {

        try {
            SparkAgentService agentService = agentFactory.getAgentService(submitWorkReq.getClusterType());
            SparkLauncher sparkLauncher = agentService.getSparkLauncher(submitWorkReq);
            String appId = agentService.submitWork(sparkLauncher);
            return SubmitWorkRes.builder().appId(appId).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    public SubmitWorkRes submitWorkForPySpark(SubmitWorkReq submitWorkReq) {

        try {
            SparkAgentService agentService = agentFactory.getAgentService(submitWorkReq.getClusterType());
            SparkLauncher sparkLauncher = agentService.getSparkLauncher(submitWorkReq);
            Map<String, String> result = agentService.submitWorkForPySpark(sparkLauncher);
            return SubmitWorkRes.builder().result(result).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    public GetWorkInfoRes getWorkInfo(GetWorkStatusReq getWorkStatusReq) {

        try {
            SparkAgentService agentService = agentFactory.getAgentService(getWorkStatusReq.getClusterType());
            return agentService.getWorkInfo(getWorkStatusReq.getAppId(), getWorkStatusReq.getSparkHomePath());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    public GetWorkStderrLogRes getWorkStderrLog(GetWorkStderrLogReq getWorkStderrLogReq) {

        try {
            SparkAgentService agentService = agentFactory.getAgentService(getWorkStderrLogReq.getClusterType());
            String appLog =
                agentService.getStderrLog(getWorkStderrLogReq.getAppId(), getWorkStderrLogReq.getSparkHomePath());
            return GetWorkStderrLogRes.builder().log(appLog).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    public GetWorkStdoutLogRes getWorkStdoutLog(GetWorkStdoutLogReq getWorkStdoutLogReq) {

        try {
            SparkAgentService agentService = agentFactory.getAgentService(getWorkStdoutLogReq.getClusterType());
            String appLog =
                agentService.getStdoutLog(getWorkStdoutLogReq.getAppId(), getWorkStdoutLogReq.getSparkHomePath());
            return GetWorkStdoutLogRes.builder().log(appLog).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    public GetWorkStdoutLogRes getCustomJarWorkStdoutLog(GetWorkStdoutLogReq getWorkStdoutLogReq) {

        try {
            SparkAgentService agentService = agentFactory.getAgentService(getWorkStdoutLogReq.getClusterType());
            String appLog = agentService.getCustomJarStdoutLog(getWorkStdoutLogReq.getAppId(),
                getWorkStdoutLogReq.getSparkHomePath());
            return GetWorkStdoutLogRes.builder().log(appLog).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }

    }

    public GetWorkStdoutLogRes getLastLineWorkStdoutLog(GetWorkStdoutLogReq getWorkStdoutLogReq) {

        try {
            SparkAgentService agentService = agentFactory.getAgentService(getWorkStdoutLogReq.getClusterType());
            String appLog =
                agentService.getStdoutLog(getWorkStdoutLogReq.getAppId(), getWorkStdoutLogReq.getSparkHomePath());

            // 只截取后1行的日志,用于打印
            appLog = appLog.replace("End of LogType:stdout", "").replace("LogType:stdout-start", "");
            String[] split = appLog.split("\n");
            List<String> list = Arrays.asList(split);
            list = list.subList(list.size() > 1 ? list.size() - 1 : 0, list.size());
            StringBuilder stringBuilder = new StringBuilder();
            for (String str : list) {
                stringBuilder.append(str).append("\n");
            }
            return GetWorkStdoutLogRes.builder().log(stringBuilder.toString()).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    public GetWorkDataRes getWorkData(GetWorkDataReq getWorkDataReq) {

        try {
            SparkAgentService agentService = agentFactory.getAgentService(getWorkDataReq.getClusterType());
            String workDataStr =
                agentService.getWorkDataStr(getWorkDataReq.getAppId(), getWorkDataReq.getSparkHomePath());
            return GetWorkDataRes.builder().data(JSON.parseArray(workDataStr, List.class)).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    public void stopWork(StopWorkReq stopWorkReq) {

        try {
            SparkAgentService agentService = agentFactory.getAgentService(stopWorkReq.getClusterType());
            agentService.stopWork(stopWorkReq.getAppId(), stopWorkReq.getSparkHomePath(),
                stopWorkReq.getAgentHomePath());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    public NodeMonitorInfo getNodeMonitor() {

        try {
            NetworkSnapshot firstNetworkSnapshot = readNetworkSnapshot();
            DiskSnapshot firstDiskSnapshot = readDiskSnapshot();
            Thread.sleep(1000);
            NetworkSnapshot secondNetworkSnapshot = readNetworkSnapshot();
            DiskSnapshot secondDiskSnapshot = readDiskSnapshot();

            return NodeMonitorInfo.builder().status(MonitorStatus.SUCCESS).log("检测完成")
                .usedMemorySize(getUsedMemorySize()).usedStorageSize(getUsedStorageSize())
                .cpuPercent(getCpuPercent())
                .networkIoReadSpeed(round((secondNetworkSnapshot.readBytes() - firstNetworkSnapshot.readBytes())
                    / 1024.0))
                .networkIoWriteSpeed(round((secondNetworkSnapshot.writeBytes() - firstNetworkSnapshot.writeBytes())
                    / 1024.0))
                .diskIoReadSpeed(round((secondDiskSnapshot.readSectors() - firstDiskSnapshot.readSectors()) * 512
                    / 1024.0))
                .diskIoWriteSpeed(round((secondDiskSnapshot.writeSectors() - firstDiskSnapshot.writeSectors()) * 512
                    / 1024.0))
                .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return NodeMonitorInfo.builder().status(MonitorStatus.FAIL).log(e.getMessage()).build();
        }
    }

    public static int findUnusedPort() {

        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException("未存在可使用端口号");
        }
    }

    public ContainerCheckRes containerCheck(ContainerCheckReq containerCheckReq) {

        try {
            return new RestTemplate()
                .getForEntity("http://127.0.0.1:" + containerCheckReq.getPort() + "/check", ContainerCheckRes.class)
                .getBody();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ContainerCheckRes.builder().code("500").msg(e.getMessage()).build();
        }
    }

    public ExecuteContainerSqlRes executeContainerSql(ExecuteContainerSqlReq executeContainerSqlReq) {

        try {
            ContainerGetDataReq containerGetDataReq = ContainerGetDataReq.builder().sql(executeContainerSqlReq.getSql())
                .limit(executeContainerSqlReq.getLimit()).build();

            ResponseEntity<ExecuteContainerSqlRes> forEntity =
                new RestTemplate().postForEntity("http://127.0.0.1:" + executeContainerSqlReq.getPort() + "/getData",
                    containerGetDataReq, ExecuteContainerSqlRes.class);
            return forEntity.getBody();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ExecuteContainerSqlRes.builder().code("500").msg(e.getMessage()).build();
        }
    }

    public DeployContainerRes deployContainer(SubmitWorkReq submitWorkReq) {

        try {
            SparkAgentService agentService = agentFactory.getAgentService(submitWorkReq.getClusterType());
            int port = findUnusedPort();
            submitWorkReq.getPluginReq().setContainerPort(port);
            SparkLauncher sparkLauncher = agentService.getSparkLauncher(submitWorkReq);
            String appId = agentService.submitWork(sparkLauncher);
            return DeployContainerRes.builder().appId(appId).port(port).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    private Double getUsedMemorySize() {

        com.sun.management.OperatingSystemMXBean operatingSystemMXBean =
            (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory
                .getOperatingSystemMXBean();
        return toGb(operatingSystemMXBean.getTotalMemorySize() - operatingSystemMXBean.getFreeMemorySize());
    }

    private Double getUsedStorageSize() throws IOException {

        long usedStorageSize = 0;
        for (FileStore fileStore : FileSystems.getDefault().getFileStores()) {
            usedStorageSize += fileStore.getTotalSpace() - fileStore.getUsableSpace();
        }
        return toGb(usedStorageSize);
    }

    private Double getCpuPercent() {

        com.sun.management.OperatingSystemMXBean operatingSystemMXBean =
            (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory
                .getOperatingSystemMXBean();
        double cpuLoad = operatingSystemMXBean.getCpuLoad();
        return cpuLoad < 0 ? 0.0 : round(cpuLoad * 100);
    }

    private NetworkSnapshot readNetworkSnapshot() throws IOException {

        Path procNetDev = Path.of("/proc/net/dev");
        if (!Files.exists(procNetDev)) {
            return new NetworkSnapshot(0, 0);
        }

        long readBytes = 0;
        long writeBytes = 0;
        for (String line : Files.readAllLines(procNetDev)) {
            if (!line.contains(":")) {
                continue;
            }
            String[] columns = line.substring(line.indexOf(":") + 1).trim().split("\\s+");
            if (columns.length < 16) {
                continue;
            }
            readBytes += Long.parseLong(columns[0]);
            writeBytes += Long.parseLong(columns[8]);
        }
        return new NetworkSnapshot(readBytes, writeBytes);
    }

    private DiskSnapshot readDiskSnapshot() throws IOException {

        Path procDiskStats = Path.of("/proc/diskstats");
        if (!Files.exists(procDiskStats)) {
            return new DiskSnapshot(0, 0);
        }

        long readSectors = 0;
        long writeSectors = 0;
        for (String line : Files.readAllLines(procDiskStats)) {
            String[] columns = line.trim().split("\\s+");
            if (columns.length < 14 || isPartition(columns[2])) {
                continue;
            }
            readSectors += Long.parseLong(columns[5]);
            writeSectors += Long.parseLong(columns[9]);
        }
        return new DiskSnapshot(readSectors, writeSectors);
    }

    private boolean isPartition(String deviceName) {

        return deviceName.matches(".*\\d+$") && !deviceName.startsWith("nvme") && !deviceName.startsWith("mmcblk");
    }

    private Double toGb(long bytes) {

        return round(bytes / 1024.0 / 1024.0 / 1024.0);
    }

    private Double round(double value) {

        return Math.round(value * 10.0) / 10.0;
    }

    private record NetworkSnapshot(long readBytes, long writeBytes) {
    }

    private record DiskSnapshot(long readSectors, long writeSectors) {
    }
}
