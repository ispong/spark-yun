package com.isxcode.spark.agent.run.flink.impl;

import com.alibaba.fastjson2.JSON;
import com.isxcode.spark.agent.run.flink.FlinkAgentService;
import com.isxcode.spark.agent.run.utils.AgentJavaOptions;
import com.isxcode.spark.agent.run.utils.CommandRunner;
import com.isxcode.spark.agent.run.utils.CommandRunner.CommandResult;
import com.isxcode.spark.api.agent.constants.AgentKubernetes;
import com.isxcode.spark.api.agent.constants.AgentType;
import com.isxcode.spark.api.agent.req.flink.GetWorkInfoReq;
import com.isxcode.spark.api.agent.req.flink.GetWorkLogReq;
import com.isxcode.spark.api.agent.req.flink.StopWorkReq;
import com.isxcode.spark.api.agent.req.flink.SubmitWorkReq;
import com.isxcode.spark.api.agent.res.flink.GetWorkInfoRes;
import com.isxcode.spark.api.agent.res.flink.GetWorkLogRes;
import com.isxcode.spark.api.agent.res.flink.StopWorkRes;
import com.isxcode.spark.api.agent.res.flink.SubmitWorkRes;
import com.isxcode.spark.api.work.constants.WorkType;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.client.deployment.application.ApplicationConfiguration;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.client.program.ClusterClientProvider;
import org.apache.flink.configuration.*;
import org.apache.flink.kubernetes.KubernetesClusterClientFactory;
import org.apache.flink.kubernetes.KubernetesClusterDescriptor;
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions;
import org.apache.flink.kubernetes.configuration.KubernetesDeploymentTarget;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FlinkKubernetesAgentService implements FlinkAgentService {

    private static final int MAX_LOG_CHARS = 30000;

    private static final Duration KUBECTL_TIMEOUT = Duration.ofSeconds(30);

    private static final String KUBERNETES_NODE_NAME_KEY = "qing.kubernetes.node.name";

    @Override
    public String getAgentType() {
        return AgentType.K8S;
    }

    public void generatePodTemplate(List<String> hostList, List<String> volumeMounts, List<String> volumes,
        String agentHomePath, String workInstanceId, String nodeName) throws IOException {

        String podTemplateContent = buildPodTemplate(hostList, volumeMounts, volumes, nodeName);

        // 判断pod文件夹是否存在
        if (!new File(agentHomePath + File.separator + "pod").exists()) {
            Files.createDirectories(Paths.get(agentHomePath + File.separator + "pod"));
        }

        // 判断k8s-logs文件夹是否存在
        if (!new File(agentHomePath + File.separator + "k8s-logs").exists()) {
            Files.createDirectories(Paths.get(agentHomePath + File.separator + "k8s-logs"));
        }

        // 创建日志目录
        Path k8sLog = Paths.get(agentHomePath + File.separator + "k8s-logs" + File.separator + workInstanceId);
        Files.createDirectories(k8sLog);
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_WRITE);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        Files.setPosixFilePermissions(k8sLog, perms);

        // 创建pod文件
        try (InputStream inputStream = new ByteArrayInputStream(podTemplateContent.getBytes(StandardCharsets.UTF_8))) {
            Files.copy(inputStream, Paths.get(agentHomePath + File.separator + "pod").resolve(workInstanceId + ".yaml"),
                StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String buildPodTemplate(List<String> hostList, List<String> volumeMounts, List<String> volumes,
        String nodeName) {

        StringBuilder podTemplate = new StringBuilder();
        podTemplate.append("apiVersion: v1\n");
        podTemplate.append("kind: Pod\n");
        podTemplate.append("metadata:\n");
        podTemplate.append("  name: pod-template\n");
        podTemplate.append("spec:\n");
        podTemplate.append("  terminationGracePeriodSeconds: 600\n");
        if (Strings.isNotEmpty(nodeName)) {
            podTemplate.append("  nodeSelector:\n");
            podTemplate.append("    kubernetes.io/hostname: ").append(yamlQuote(nodeName)).append("\n");
        }
        if (!hostList.isEmpty()) {
            podTemplate.append("  hostAliases:\n");
            hostList.forEach(podTemplate::append);
        }
        podTemplate.append("  containers:\n");
        podTemplate.append("    - name: flink-main-container\n");
        podTemplate.append("      volumeMounts:\n");
        volumeMounts.forEach(podTemplate::append);
        podTemplate.append("  volumes:\n");
        volumes.forEach(podTemplate::append);
        return podTemplate.toString();
    }

    private String resolveKubernetesNodeName(Map<String, Object> flinkConfig) {

        Object nodeName = flinkConfig.get(KUBERNETES_NODE_NAME_KEY);
        if (nodeName == null || Strings.isEmpty(String.valueOf(nodeName))) {
            return System.getenv("KUBERNETES_NODE_NAME");
        }
        return String.valueOf(nodeName);
    }

    private String yamlQuote(String value) {

        return "\"" + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Override
    public SubmitWorkRes submitWork(SubmitWorkReq submitWorkReq) throws Exception {

        Configuration flinkConfig = GlobalConfiguration.loadConfiguration(submitWorkReq.getFlinkHome() + "/conf");

        // flink的args配置
        if (WorkType.FLINK_JAR.equals(submitWorkReq.getWorkType())) {
            String[] args = submitWorkReq.getPluginReq() == null || submitWorkReq.getPluginReq().getArgs() == null
                ? new String[0]
                : submitWorkReq.getPluginReq().getArgs();
            flinkConfig.set(ApplicationConfiguration.APPLICATION_ARGS,
                Arrays.asList(args));
        } else {
            String pluginConfig = submitWorkReq.getPluginReq() == null ? "{}" : JSON.toJSONString(submitWorkReq.getPluginReq());
            flinkConfig.set(ApplicationConfiguration.APPLICATION_ARGS, Collections.singletonList(Base64.getEncoder()
                .encodeToString(pluginConfig.getBytes(StandardCharsets.UTF_8))));
        }

        // 配置名称
        flinkConfig.set(PipelineOptions.NAME, submitWorkReq.getFlinkSubmit().getAppName() + "-"
            + submitWorkReq.getWorkType() + "-" + submitWorkReq.getWorkId() + "-" + submitWorkReq.getWorkInstanceId());

        // flink on k8s配置
        flinkConfig.set(ApplicationConfiguration.APPLICATION_MAIN_CLASS,
            submitWorkReq.getFlinkSubmit().getEntryClass());
        flinkConfig.set(DeploymentOptions.TARGET, KubernetesDeploymentTarget.APPLICATION.getName());
        flinkConfig.set(DeploymentOptionsInternal.CONF_DIR, submitWorkReq.getFlinkHome() + "/conf");
        flinkConfig.set(PipelineOptions.JARS, Collections.singletonList("local:///opt/flink/examples/app.jar"));
        flinkConfig.set(KubernetesConfigOptions.CLUSTER_ID, "zhiqingyun-cluster-" + System.currentTimeMillis());
        flinkConfig.set(KubernetesConfigOptions.REST_SERVICE_EXPOSED_TYPE,
            KubernetesConfigOptions.ServiceExposedType.NodePort);
        flinkConfig.set(KubernetesConfigOptions.CONTAINER_IMAGE_PULL_POLICY,
            KubernetesConfigOptions.ImagePullPolicy.IfNotPresent);
        flinkConfig.set(KubernetesConfigOptions.NAMESPACE, AgentKubernetes.NAMESPACE);
        flinkConfig.set(KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT, AgentKubernetes.SERVICE_ACCOUNT_NAME);
        flinkConfig.set(KubernetesConfigOptions.CONTAINER_IMAGE, AgentKubernetes.FLINK_DOCKER_IMAGE);
        flinkConfig.set(KubernetesConfigOptions.TASK_MANAGER_CPU, 2.0);
        flinkConfig.set(KubernetesConfigOptions.KUBERNETES_POD_TEMPLATE, submitWorkReq.getAgentHomePath()
            + File.separator + "pod" + File.separator + submitWorkReq.getWorkInstanceId() + ".yaml");
        flinkConfig.set(KubernetesConfigOptions.KUBERNETES_HOSTNETWORK_ENABLED, true);
        flinkConfig.set(KubernetesConfigOptions.FLINK_LOG_DIR, "/tmp/log");
        flinkConfig.set(JobManagerOptions.TOTAL_PROCESS_MEMORY, MemorySize.parse("1g"));
        flinkConfig.set(TaskManagerOptions.TOTAL_PROCESS_MEMORY, MemorySize.parse("1g"));
        flinkConfig.set(TaskManagerOptions.NUM_TASK_SLOTS, 1);
        flinkConfig.set(RestartStrategyOptions.RESTART_STRATEGY, "disable");
        flinkConfig.setString("kubernetes.client.shutdown-timeout", "30000");

        Map<String, Object> pluginFlinkConfig = submitWorkReq.getFlinkSubmit().getConf() == null
            ? new HashMap<>()
            : new HashMap<>(submitWorkReq.getFlinkSubmit().getConf());
        submitWorkReq.getFlinkSubmit().setConf(pluginFlinkConfig);
        pluginFlinkConfig.forEach((k, v) -> {
            if (k.startsWith("qing.")) {
                return;
            }
            if (v != null) {
                flinkConfig.setString(k, String.valueOf(v));
            } else {
                throw new IllegalArgumentException("Unsupported type for key: " + k + ", value: " + v);
            }
        });
        appendJava17ModuleOptions(flinkConfig);

        // 映射文件路径
        List<String> volumeMounts = new ArrayList<>();
        String volumeTemplate = "   - name: %s\n      hostPath:\n        path: %s\n";
        List<String> volumes = new ArrayList<>();
        String volumeMountsTemplate = "       - name: %s\n" + "          mountPath: %s\n";

        // app文件映射
        volumeMounts.add(String.format(volumeMountsTemplate, "app", "/opt/flink/examples/app.jar"));
        if (WorkType.FLINK_JAR.equals(submitWorkReq.getWorkType())) {
            volumes.add(String.format(volumeTemplate, "app", submitWorkReq.getAgentHomePath() + File.separator + "file"
                + File.separator + submitWorkReq.getFlinkSubmit().getAppResource()));
        } else {
            volumes.add(String.format(volumeTemplate, "app", submitWorkReq.getAgentHomePath() + File.separator
                + "plugins" + File.separator + submitWorkReq.getFlinkSubmit().getAppResource()));
        }

        // 日志文件映射
        volumeMounts.add(String.format(volumeMountsTemplate, "flink-log", "/tmp/log"));
        volumes.add(String.format(volumeTemplate, "flink-log", submitWorkReq.getAgentHomePath() + File.separator
            + "k8s-logs" + File.separator + submitWorkReq.getWorkInstanceId()));

        // 至轻云lib映射
        File[] jarFiles = new File(submitWorkReq.getAgentHomePath() + File.separator + "lib").listFiles();
        if (jarFiles != null) {
            for (int i = 0; i < jarFiles.length; i++) {
                volumeMounts.add(String.format(volumeMountsTemplate, "zhiqingyun-lib-" + i,
                    "/opt/flink/lib/" + jarFiles[i].getName()));
                volumes.add(String.format(volumeTemplate, "zhiqingyun-lib-" + i, submitWorkReq.getAgentHomePath()
                    + File.separator + "lib" + File.separator + jarFiles[i].getName()));
            }
        }

        // 自定义依赖
        if (submitWorkReq.getLibConfig() != null) {
            for (int i = 0; i < submitWorkReq.getLibConfig().size(); i++) {
                volumeMounts.add(String.format(volumeMountsTemplate, "lib-" + i,
                    "/opt/flink/lib/" + submitWorkReq.getLibConfig().get(i) + ".jar"));
                volumes.add(String.format(volumeTemplate, "lib-" + i, submitWorkReq.getAgentHomePath() + File.separator
                    + "file" + File.separator + submitWorkReq.getLibConfig().get(i) + ".jar"));
            }
        }

        // 自定义函数
        if (submitWorkReq.getFuncConfig() != null) {
            for (int i = 0; i < submitWorkReq.getFuncConfig().size(); i++) {
                volumeMounts.add(String.format(volumeMountsTemplate, "func-" + i,
                    "/opt/flink/lib/" + submitWorkReq.getFuncConfig().get(i) + ".jar"));
                volumes.add(String.format(volumeTemplate, "func-" + i, submitWorkReq.getAgentHomePath() + File.separator
                    + "file" + File.separator + submitWorkReq.getFuncConfig().get(i).getFileId() + ".jar"));
            }
        }

        // 从flinkConfig中解析出域名映射
        Map<String, String> hostMapping = new HashMap<>();
        if ((pluginFlinkConfig.get("qing.host1.name") != null) && pluginFlinkConfig.get("qing.host1.value") != null) {
            hostMapping.put(String.valueOf(pluginFlinkConfig.get("qing.host1.name")),
                String.valueOf(pluginFlinkConfig.get("qing.host1.value")));
        }
        if ((pluginFlinkConfig.get("qing.host2.name") != null) && pluginFlinkConfig.get("qing.host2.value") != null) {
            hostMapping.put(String.valueOf(pluginFlinkConfig.get("qing.host2.name")),
                String.valueOf(pluginFlinkConfig.get("qing.host2.value")));
        }
        if ((pluginFlinkConfig.get("qing.host3.name") != null) && pluginFlinkConfig.get("qing.host3.value") != null) {
            hostMapping.put(String.valueOf(pluginFlinkConfig.get("qing.host3.name")),
                String.valueOf(pluginFlinkConfig.get("qing.host3.value")));
        }

        // 拼接host映射
        List<String> hostList = new ArrayList<>();
        if (!hostMapping.isEmpty()) {
            hostMapping.forEach(
                (k, v) -> hostList.add("    - ip: " + yamlQuote(v) + "\n      hostnames:\n" + "        - "
                    + yamlQuote(k) + "\n"));
        }
        String nodeName = resolveKubernetesNodeName(pluginFlinkConfig);
        pluginFlinkConfig.remove(KUBERNETES_NODE_NAME_KEY);

        // 生成pod文件
        generatePodTemplate(hostList, volumeMounts, volumes, submitWorkReq.getAgentHomePath(),
            submitWorkReq.getWorkInstanceId(), nodeName);

        // 提交flink作业
        ClusterSpecification clusterSpecification =
            new ClusterSpecification.ClusterSpecificationBuilder().setMasterMemoryMB(1024).setTaskManagerMemoryMB(1024)
                .setSlotsPerTaskManager(2).createClusterSpecification();

        ApplicationConfiguration applicationConfiguration = ApplicationConfiguration.fromConfiguration(flinkConfig);
        applicationConfiguration.applyToConfiguration(flinkConfig);
        KubernetesClusterClientFactory kubernetesClusterClientFactory = new KubernetesClusterClientFactory();
        try (KubernetesClusterDescriptor clusterDescriptor =
            kubernetesClusterClientFactory.createClusterDescriptor(flinkConfig)) {
            ClusterClientProvider<String> clusterClientProvider =
                clusterDescriptor.deployApplicationCluster(clusterSpecification, applicationConfiguration);
            try (ClusterClient<String> clusterClient = clusterClientProvider.getClusterClient()) {
                return SubmitWorkRes.builder().webUrl(clusterClient.getWebInterfaceURL())
                    .appId(String.valueOf(clusterClient.getClusterId())).build();
            }
        }
    }

    private void appendJava17ModuleOptions(Configuration flinkConfig) {

        appendJava17ModuleOptions(flinkConfig, "env.java.opts.all");
        appendJava17ModuleOptions(flinkConfig, "env.java.opts.jobmanager");
        appendJava17ModuleOptions(flinkConfig, "env.java.opts.taskmanager");
    }

    private void appendJava17ModuleOptions(Configuration flinkConfig, String key) {

        flinkConfig.setString(key,
            AgentJavaOptions.mergeOptions(flinkConfig.getString(key, ""), AgentJavaOptions.FLINK_JAVA_17_MODULE_OPTIONS));
    }

    @Override
    public GetWorkInfoRes getWorkInfo(GetWorkInfoReq getWorkInfoReq) throws Exception {

        String agentHome = resolveAgentHome(getWorkInfoReq.getAgentHome());
        String logFinalState =
            getApplicationFinalState(resolveKubernetesLogDir(agentHome, getWorkInfoReq.getWorkInstanceId()),
                getWorkInfoReq.getAppId());
        if (Strings.isEmpty(logFinalState) && Strings.isEmpty(getWorkInfoReq.getWorkInstanceId())) {
            logFinalState = getApplicationFinalState(agentHome, getWorkInfoReq.getAppId());
        }
        if (Strings.isNotEmpty(logFinalState)) {
            return GetWorkInfoRes.builder().finalState(logFinalState).appId(getWorkInfoReq.getAppId()).build();
        }

        List<String> podStatus = new ArrayList<>();
        CommandResult result = CommandRunner.run(
            Arrays.asList("kubectl", "get", "pods", "-l", "app=" + getWorkInfoReq.getAppId(), "-n",
                AgentKubernetes.NAMESPACE),
            KUBECTL_TIMEOUT);
        for (String line : result.getStdout().split("\n")) {
            Matcher matcher = Pattern.compile("\\s+\\d/\\d\\s+(\\w+)").matcher(line);
            if (matcher.find()) {
                podStatus.add(matcher.group(1));
            }
        }
        if (!podStatus.isEmpty()) {
            return GetWorkInfoRes.builder().finalState(resolvePodFinalState(podStatus)).appId(getWorkInfoReq.getAppId())
                .build();
        }
        if (result.getOutput().contains("No resources found in " + AgentKubernetes.NAMESPACE + " namespace")) {
            return GetWorkInfoRes.builder().finalState("Over").appId(getWorkInfoReq.getAppId()).build();
        }
        if (!result.isSuccess()) {
            throw new Exception("Command execution failed:\n" + result.getOutput());
        }

        throw new Exception("获取状态异常");
    }

    @Override
    public GetWorkLogRes getWorkLog(GetWorkLogReq getWorkLogReq) throws Exception {

        StringBuilder logBuilder = new StringBuilder();
        appendLocalKubernetesLogs(logBuilder,
            resolveKubernetesLogDir(resolveAgentHome(getWorkLogReq.getAgentHomePath()), getWorkLogReq.getWorkInstanceId()));
        if (Strings.isEmpty(logBuilder.toString())) {
            appendKubectlLogs(logBuilder, getWorkLogReq.getAppId());
        }

        return GetWorkLogRes.builder().log(tail(logBuilder.toString())).build();
    }

    private void appendLocalKubernetesLogs(StringBuilder logBuilder, File logDir) throws IOException {

        File[] logFiles = logDir.listFiles(File::isFile);
        if (logFiles != null) {
            Arrays.sort(logFiles, Comparator.comparing(File::getName));
            for (File logFile : logFiles) {
                logBuilder.append("===== ").append(logFile.getName()).append(" =====\n");
                try (
                    BufferedReader bufferedReader = Files.newBufferedReader(logFile.toPath(), StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        logBuilder.append(line).append("\n");
                    }
                }
            }
        }
    }

    @Override
    public StopWorkRes stopWork(StopWorkReq stopWorkReq) throws Exception {

        String flinkConfDir =
            Strings.isEmpty(stopWorkReq.getFlinkHome()) ? null : stopWorkReq.getFlinkHome() + File.separator + "conf";
        Configuration flinkConfig =
            Strings.isEmpty(flinkConfDir) ? GlobalConfiguration.loadConfiguration()
                : GlobalConfiguration.loadConfiguration(flinkConfDir);
        flinkConfig.set(DeploymentOptions.TARGET, KubernetesDeploymentTarget.APPLICATION.getName());
        flinkConfig.set(KubernetesConfigOptions.NAMESPACE, AgentKubernetes.NAMESPACE);
        flinkConfig.set(KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT, AgentKubernetes.SERVICE_ACCOUNT_NAME);

        KubernetesClusterClientFactory kubernetesClusterClientFactory = new KubernetesClusterClientFactory();
        try (KubernetesClusterDescriptor clusterDescriptor =
            kubernetesClusterClientFactory.createClusterDescriptor(flinkConfig)) {
            clusterDescriptor.killCluster(stopWorkReq.getAppId());
            return StopWorkRes.builder().build();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return StopWorkRes.builder().build();
            }
            throw e;
        }
    }

    private String resolveAgentHome(String agentHome) {

        if (Strings.isEmpty(agentHome)) {
            return "";
        }
        File agentHomeFile = new File(agentHome);
        if (new File(agentHomeFile, "k8s-logs").exists()) {
            return agentHome;
        }
        File zhiqingyunAgentHome = new File(agentHomeFile, "zhiqingyun-agent");
        if (new File(zhiqingyunAgentHome, "k8s-logs").exists()) {
            return zhiqingyunAgentHome.getAbsolutePath();
        }
        return agentHome;
    }

    private String getApplicationFinalState(String agentHome, String appId) throws IOException {

        String log = findApplicationLog(agentHome, appId);
        return parseApplicationFinalState(log);
    }

    private String getApplicationFinalState(File logDir, String appId) throws IOException {

        String log = findApplicationLog(logDir, appId);
        return parseApplicationFinalState(log);
    }

    private String parseApplicationFinalState(String log) {

        if (Strings.isEmpty(log)) {
            return null;
        }
        if (log.contains("Application completed SUCCESSFULLY") || log.contains("application status SUCCEEDED")
            || log.contains("switched from state RUNNING to FINISHED")) {
            return "SUCCEEDED";
        }
        if (log.contains("Application failed unexpectedly") || log.contains("application status FAILED")
            || log.contains("Could not execute application")) {
            return "FAILED";
        }
        return null;
    }

    private String findApplicationLog(String agentHome, String appId) throws IOException {

        File logRoot = new File(agentHome + File.separator + "k8s-logs");
        File[] workLogDirs = logRoot.listFiles(File::isDirectory);
        if (workLogDirs == null) {
            return "";
        }
        StringBuilder logBuilder = new StringBuilder();
        for (File workLogDir : workLogDirs) {
            File[] logFiles = workLogDir.listFiles(File::isFile);
            if (logFiles == null) {
                continue;
            }
            for (File logFile : logFiles) {
                if (!logFile.getName().contains("application")) {
                    continue;
                }
                String logContent = Files.readString(logFile.toPath(), StandardCharsets.UTF_8);
                if (logContent.contains(appId)) {
                    logBuilder.append(logContent).append("\n");
                }
            }
        }
        return tail(logBuilder.toString());
    }

    private String findApplicationLog(File workLogDir, String appId) throws IOException {

        File[] logFiles = workLogDir.listFiles(File::isFile);
        if (logFiles == null) {
            return "";
        }
        StringBuilder logBuilder = new StringBuilder();
        for (File logFile : logFiles) {
            if (!logFile.getName().contains("application")) {
                continue;
            }
            String logContent = Files.readString(logFile.toPath(), StandardCharsets.UTF_8);
            if (logContent.contains(appId)) {
                logBuilder.append(logContent).append("\n");
            }
        }
        return tail(logBuilder.toString());
    }

    private File resolveKubernetesLogDir(String agentHome, String workInstanceId) {

        return new File(agentHome + File.separator + "k8s-logs" + File.separator + workInstanceId);
    }

    private String resolvePodFinalState(List<String> podStatus) {

        for (String status : podStatus) {
            if (isRunningPodStatus(status)) {
                return status;
            }
        }
        for (String status : podStatus) {
            if ("COMPLETED".equalsIgnoreCase(status) || "SUCCEEDED".equalsIgnoreCase(status)) {
                return "SUCCEEDED";
            }
        }
        for (String status : podStatus) {
            if ("ERROR".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status)
                || "CRASHLOOPBACKOFF".equalsIgnoreCase(status)) {
                return "FAILED";
            }
        }
        return podStatus.get(0);
    }

    private boolean isRunningPodStatus(String status) {

        return "RUNNING".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status)
            || "CONTAINERCREATING".equalsIgnoreCase(status) || "TERMINATING".equalsIgnoreCase(status)
            || "INITIALIZING".equalsIgnoreCase(status);
    }

    private void appendKubectlLogs(StringBuilder logBuilder, String appId) throws IOException, InterruptedException {

        CommandResult result = CommandRunner.run(Arrays.asList("kubectl", "logs", "-n", AgentKubernetes.NAMESPACE, "-l",
            "app=" + appId, "--all-containers=true", "--tail=2000"), KUBECTL_TIMEOUT);
        logBuilder.append(result.getOutput());
    }

    private String tail(String log) {

        if (log.length() <= MAX_LOG_CHARS) {
            return log;
        }
        return log.substring(log.length() - MAX_LOG_CHARS);
    }
}
