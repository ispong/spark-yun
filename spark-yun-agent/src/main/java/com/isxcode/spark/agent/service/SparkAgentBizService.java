package com.isxcode.spark.agent.service;

import com.alibaba.fastjson.JSON;
import com.isxcode.spark.agent.run.utils.CommandRunner;
import com.isxcode.spark.agent.run.utils.CommandRunner.CommandResult;
import com.isxcode.spark.agent.run.spark.SparkAgentFactory;
import com.isxcode.spark.agent.run.spark.SparkAgentService;
import com.isxcode.spark.api.agent.req.spark.*;
import com.isxcode.spark.api.agent.res.spark.*;
import com.isxcode.spark.api.monitor.constants.MonitorStatus;
import com.isxcode.spark.api.monitor.dto.NodeMonitorInfo;
import com.isxcode.spark.api.work.res.AgentLinkResponse;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
                .usedMemorySize(getUsedMemorySize()).usedStorageSize(getUsedStorageSize()).cpuPercent(getCpuPercent())
                .networkIoReadSpeed(
                    round((secondNetworkSnapshot.readBytes() - firstNetworkSnapshot.readBytes()) / 1024.0))
                .networkIoWriteSpeed(
                    round((secondNetworkSnapshot.writeBytes() - firstNetworkSnapshot.writeBytes()) / 1024.0))
                .diskIoReadSpeed(
                    round((secondDiskSnapshot.readSectors() - firstDiskSnapshot.readSectors()) * 512 / 1024.0))
                .diskIoWriteSpeed(
                    round((secondDiskSnapshot.writeSectors() - firstDiskSnapshot.writeSectors()) * 512 / 1024.0))
                .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return NodeMonitorInfo.builder().status(MonitorStatus.FAIL).log(e.getMessage()).build();
        }
    }

    public AgentLinkResponse cleanAgent(CleanAgentReq cleanAgentReq) {

        try {
            String username = resolveCleanUsername(cleanAgentReq);
            StringBuilder cleanLog = new StringBuilder();

            cleanHadoopLocalFileCache(username, cleanLog);
            cleanSparkStaging(username, cleanLog);
            cleanKubernetesPods(cleanLog);
            cleanDockerPods(cleanLog);

            return AgentLinkResponse.builder().msg("清理成功").log(cleanLog.toString()).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    public AgentLinkResponse uploadAgentFile(UploadAgentFileReq uploadAgentFileReq) {

        try {
            Path targetFile = resolveAgentFile(uploadAgentFileReq.getAgentHomePath(), uploadAgentFileReq.getDirectory(),
                uploadAgentFileReq.getFileName());
            Files.createDirectories(targetFile.getParent());
            Files.write(targetFile, Base64.getDecoder().decode(uploadAgentFileReq.getContentBase64()));
            return AgentLinkResponse.builder().msg("上传成功").build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    public AgentLinkResponse submitLocalScript(SubmitLocalScriptReq submitLocalScriptReq) {

        try {
            Path scriptFile = resolveWorkFile(submitLocalScriptReq.getAgentHomePath(),
                submitLocalScriptReq.getWorkInstanceId(), submitLocalScriptReq.getScriptSuffix());
            Path logFile = resolveWorkFile(submitLocalScriptReq.getAgentHomePath(),
                submitLocalScriptReq.getWorkInstanceId(), ".log");
            Files.createDirectories(scriptFile.getParent());
            Files.writeString(scriptFile, submitLocalScriptReq.getScript(), StandardCharsets.UTF_8);

            String executeCommand = "source /etc/profile >/dev/null 2>&1; nohup "
                + resolveScriptCommand(submitLocalScriptReq.getCommand()) + " " + shellQuote(scriptFile.toString())
                + " >> " + shellQuote(logFile.toString()) + " 2>&1 < /dev/null & echo $!";
            CommandResult result =
                CommandRunner.run(List.of("bash", "-lc", executeCommand), Duration.ofSeconds(30));
            if (!result.isSuccess()) {
                throw new IsxAppException(result.getOutput());
            }

            String pid = result.getStdout().trim();
            return AgentLinkResponse.builder().msg("提交成功").instanceId(pid).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    public AgentLinkResponse getLocalScriptStatus(LocalScriptStatusReq localScriptStatusReq) {

        try {
            CommandResult result = CommandRunner.run(List.of("ps", "-p", localScriptStatusReq.getPid()),
                Duration.ofSeconds(10));
            return AgentLinkResponse.builder()
                .finalState(result.getStdout().contains(localScriptStatusReq.getPid()) ? "RUNNING" : "FINISHED")
                .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    public AgentLinkResponse getLocalScriptLog(LocalScriptLogReq localScriptLogReq) {

        try {
            Path logFile = resolveWorkFile(localScriptLogReq.getAgentHomePath(), localScriptLogReq.getWorkInstanceId(),
                ".log");
            String scriptLog = Files.exists(logFile) ? Files.readString(logFile, StandardCharsets.UTF_8) : "";
            return AgentLinkResponse.builder().log(scriptLog).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    public AgentLinkResponse cleanLocalScript(CleanLocalScriptReq cleanLocalScriptReq) {

        try {
            Files.deleteIfExists(resolveWorkFile(cleanLocalScriptReq.getAgentHomePath(),
                cleanLocalScriptReq.getWorkInstanceId(), cleanLocalScriptReq.getScriptSuffix()));
            Files.deleteIfExists(resolveWorkFile(cleanLocalScriptReq.getAgentHomePath(),
                cleanLocalScriptReq.getWorkInstanceId(), ".log"));
            return AgentLinkResponse.builder().msg("清理成功").build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    public AgentLinkResponse stopLocalScript(StopLocalScriptReq stopLocalScriptReq) {

        try {
            CommandResult result =
                CommandRunner.run(List.of("kill", "-9", stopLocalScriptReq.getPid()), Duration.ofSeconds(10));
            if (!result.isSuccess() && !result.getOutput().contains("No such process")) {
                throw new IsxAppException(result.getOutput());
            }
            return AgentLinkResponse.builder().msg("中止成功").build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
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

    private String resolveCleanUsername(CleanAgentReq cleanAgentReq) {

        if (cleanAgentReq != null && cleanAgentReq.getUsername() != null && !cleanAgentReq.getUsername().isBlank()) {
            return cleanAgentReq.getUsername();
        }

        return System.getProperty("user.name");
    }

    private Path resolveAgentFile(String agentHomePath, String directory, String fileName) {

        if (!"file".equals(directory) && !"works".equals(directory)) {
            throw new IsxAppException("不支持的上传目录");
        }

        Path agentHome = Path.of(agentHomePath).toAbsolutePath().normalize();
        Path targetDir = agentHome.resolve(directory).normalize();
        Path targetFile = targetDir.resolve(Path.of(fileName).getFileName()).normalize();
        if (!targetFile.startsWith(targetDir)) {
            throw new IsxAppException("文件名不合法");
        }

        return targetFile;
    }

    private Path resolveWorkFile(String agentHomePath, String workInstanceId, String suffix) {

        if (!".sh".equals(suffix) && !".py".equals(suffix) && !".log".equals(suffix)) {
            throw new IsxAppException("脚本后缀不合法");
        }

        String safeWorkInstanceId = Path.of(workInstanceId).getFileName().toString();
        return resolveAgentFile(agentHomePath, "works", safeWorkInstanceId + suffix);
    }

    private String resolveScriptCommand(String command) {

        if (!"sh".equals(command) && !"python3".equals(command)) {
            throw new IsxAppException("脚本命令不合法");
        }

        return command;
    }

    private String shellQuote(String value) {

        return "'" + value.replace("'", "'\\''") + "'";
    }

    private void cleanHadoopLocalFileCache(String username, StringBuilder cleanLog) throws IOException {

        Path tmpDir = Path.of("/tmp");
        if (!Files.exists(tmpDir)) {
            cleanLog.append("/tmp目录不存在，跳过本地缓存清理\n");
            return;
        }

        try (Stream<Path> hadoopDirs = Files.list(tmpDir)) {
            List<Path> fileCachePaths = hadoopDirs
                .filter(path -> Files.isDirectory(path) && path.getFileName().toString().startsWith("hadoop-"))
                .map(path -> path.resolve("nm-local-dir").resolve("usercache").resolve(username).resolve("filecache"))
                .filter(Files::exists).toList();

            for (Path fileCachePath : fileCachePaths) {
                deleteRecursively(fileCachePath);
                cleanLog.append("已清理本地缓存: ").append(fileCachePath).append("\n");
            }

            if (fileCachePaths.isEmpty()) {
                cleanLog.append("未发现本地Hadoop filecache缓存\n");
            }
        }
    }

    private void cleanSparkStaging(String username, StringBuilder cleanLog) throws IOException, InterruptedException {

        if (!commandExists("hadoop")) {
            cleanLog.append("未安装hadoop命令，跳过HDFS Spark缓存清理\n");
            return;
        }

        CommandResult result =
            CommandRunner.run(List.of("hadoop", "fs", "-rm", "-r", "/user/" + username + "/.sparkStaging"),
                Duration.ofMinutes(2));
        if (result.isSuccess()) {
            cleanLog.append("已清理HDFS Spark缓存\n");
            return;
        }

        cleanLog.append("HDFS Spark缓存清理返回: ").append(result.getOutput()).append("\n");
    }

    private void cleanKubernetesPods(StringBuilder cleanLog) throws IOException, InterruptedException {

        if (!commandExists("kubectl")) {
            cleanLog.append("未安装kubectl命令，跳过Kubernetes容器清理\n");
            return;
        }

        CommandResult result = CommandRunner.run(
            List.of("kubectl", "delete", "--all", "pods", "--namespace=zhiqingyun-space"), Duration.ofMinutes(2));
        cleanLog.append(result.isSuccess() ? "已清理Kubernetes容器\n"
            : "Kubernetes容器清理返回: " + result.getOutput() + "\n");
    }

    private void cleanDockerPods(StringBuilder cleanLog) throws IOException, InterruptedException {

        if (!commandExists("docker")) {
            cleanLog.append("未安装docker命令，跳过Docker容器清理\n");
            return;
        }

        CommandResult result = CommandRunner.run(List.of("sh", "-c", "containers=$(docker ps -a "
            + "| grep 'k8s_POD_zhiqingyun-*' | awk '{print $1}'); [ -z \"$containers\" ] || docker rm $containers"),
            Duration.ofMinutes(2));
        cleanLog.append(result.isSuccess() ? "已清理Docker容器\n" : "Docker容器清理返回: " + result.getOutput() + "\n");
    }

    private boolean commandExists(String command) throws IOException, InterruptedException {

        return CommandRunner.run(List.of("sh", "-c", "command -v " + command), Duration.ofSeconds(10)).isSuccess();
    }

    private void deleteRecursively(Path path) throws IOException {

        try (Stream<Path> pathStream = Files.walk(path)) {
            pathStream.sorted(Comparator.reverseOrder()).forEach(deletePath -> {
                try {
                    Files.deleteIfExists(deletePath);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (IllegalStateException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        }
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

    private record NetworkSnapshot(long readBytes, long writeBytes) {}

    private record DiskSnapshot(long readSectors, long writeSectors) {}
}
