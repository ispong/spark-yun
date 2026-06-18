package com.isxcode.spark.agent.run.flink.impl;

import com.alibaba.fastjson2.JSON;
import com.isxcode.spark.agent.run.flink.FlinkAgentService;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FlinkKubernetesAgentService implements FlinkAgentService {

    private static final int MAX_LOG_CHARS = 30000;

    private static final List<String> JAVA_17_MODULE_OPTIONS = Arrays.asList(
        "--add-exports=java.base/sun.net.util=ALL-UNNAMED",
        "--add-exports=java.rmi/sun.rmi.registry=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        "--add-exports=java.security.jgss/sun.security.krb5=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.net=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.text=ALL-UNNAMED",
        "--add-opens=java.base/java.time=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED");

    @Override
    public String getAgentType() {
        return AgentType.K8S;
    }

    public void generatePodTemplate(List<String> hostList, List<String> volumeMounts, List<String> volumes,
        String agentHomePath, String workInstanceId) throws IOException {

        String podTemplate = "apiVersion: v1 \n" + "kind: Pod \n" + "metadata: \n" + "  name: pod-template \n"
            + "spec:\n" + "  terminationGracePeriodSeconds: 600\n" + "%s" + "  containers:\n"
            + "    - name: flink-main-container\n" + "      volumeMounts:\n" + " %s" + "  volumes:\n" + " %s";

        String podTemplateContent;
        if (hostList.isEmpty()) {
            podTemplateContent =
                String.format(podTemplate, "", Strings.join(volumeMounts, ' '), Strings.join(volumes, ' '));
        } else {
            podTemplateContent = String.format(podTemplate, "  hostAliases:\n" + Strings.join(hostList, ' '),
                Strings.join(volumeMounts, ' '), Strings.join(volumes, ' '));
        }

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
        perms.add(PosixFilePermission.OTHERS_WRITE);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        Files.setPosixFilePermissions(k8sLog, perms);

        // 创建pod文件
        try (InputStream inputStream = new ByteArrayInputStream(podTemplateContent.getBytes(StandardCharsets.UTF_8))) {
            Files.copy(inputStream, Paths.get(agentHomePath + File.separator + "pod").resolve(workInstanceId + ".yaml"),
                StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public SubmitWorkRes submitWork(SubmitWorkReq submitWorkReq) throws Exception {

        Configuration flinkConfig = GlobalConfiguration.loadConfiguration(submitWorkReq.getFlinkHome() + "/conf");

        // flink的args配置
        if (WorkType.FLINK_JAR.equals(submitWorkReq.getWorkType())) {
            flinkConfig.set(ApplicationConfiguration.APPLICATION_ARGS,
                Arrays.asList(submitWorkReq.getPluginReq().getArgs()));
        } else {
            flinkConfig.set(ApplicationConfiguration.APPLICATION_ARGS, Collections.singletonList(Base64.getEncoder()
                .encodeToString(JSON.toJSONString(submitWorkReq.getPluginReq()).getBytes(StandardCharsets.UTF_8))));
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
        flinkConfig.set(KubernetesConfigOptions.NAMESPACE, "zhiqingyun-space");
        flinkConfig.set(KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT, "zhiqingyun");
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

        submitWorkReq.getFlinkSubmit().getConf().forEach((k, v) -> {
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
        Map<String, Object> pluginFlinkConfig = submitWorkReq.getFlinkSubmit().getConf();
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
                (k, v) -> hostList.add("    - ip: \"" + v + "\"\n      hostnames:\n" + "        - \"" + k + "\"\n"));
        }

        // 生成pod文件
        generatePodTemplate(hostList, volumeMounts, volumes, submitWorkReq.getAgentHomePath(),
            submitWorkReq.getWorkInstanceId());

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

        String mergedOptions = flinkConfig.getString(key, "");
        if (Strings.isEmpty(mergedOptions)) {
            mergedOptions = "";
        } else {
            mergedOptions = mergedOptions.trim();
        }
        for (String option : JAVA_17_MODULE_OPTIONS) {
            if (!mergedOptions.contains(option)) {
                mergedOptions = Strings.isEmpty(mergedOptions) ? option : mergedOptions + " " + option;
            }
        }
        flinkConfig.setString(key, mergedOptions);
    }

    @Override
    public GetWorkInfoRes getWorkInfo(GetWorkInfoReq getWorkInfoReq) throws Exception {

        String logFinalState = getApplicationFinalState(resolveAgentHome(getWorkInfoReq.getAgentHome()),
            getWorkInfoReq.getAppId());
        if (Strings.isNotEmpty(logFinalState)) {
            return GetWorkInfoRes.builder().finalState(logFinalState).appId(getWorkInfoReq.getAppId()).build();
        }

        String getStatusJobManagerFormat = "kubectl get pods -l app=%s -n zhiqingyun-space";
        String line;
        StringBuilder errLog = new StringBuilder();
        List<String> podStatus = new ArrayList<>();

        String command = String.format(getStatusJobManagerFormat, getWorkInfoReq.getAppId());
        Process process = Runtime.getRuntime().exec(command);
        try (InputStream inputStream = process.getInputStream();
            InputStream errStream = process.getErrorStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(errStream, StandardCharsets.UTF_8))) {

            while ((line = reader.readLine()) != null) {
                errLog.append(line).append("\n");
                String pattern = "\\s+\\d/\\d\\s+(\\w+)";
                Pattern regex = Pattern.compile(pattern);
                Matcher matcher = regex.matcher(line);
                if (matcher.find()) {
                    podStatus.add(matcher.group(1));
                }
            }

            if (!podStatus.isEmpty()) {
                return GetWorkInfoRes.builder().finalState(resolvePodFinalState(podStatus))
                    .appId(getWorkInfoReq.getAppId()).build();
            }

            if (errLog.toString().isEmpty()) {
                while ((line = errReader.readLine()) != null) {
                    errLog.append(line).append("\n");
                }
                if (errLog.toString().contains("No resources found in zhiqingyun-space namespace")) {
                    return GetWorkInfoRes.builder().finalState("Over").appId(getWorkInfoReq.getAppId()).build();
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 1) {
                throw new Exception("Command execution failed:\n" + errLog);
            }
        }

        throw new Exception("获取状态异常");
    }

    @Override
    public GetWorkLogRes getWorkLog(GetWorkLogReq getWorkLogReq) throws Exception {

        StringBuilder logBuilder = new StringBuilder();
        appendLocalKubernetesLogs(logBuilder,
            new File(getWorkLogReq.getAgentHomePath() + File.separator + "k8s-logs" + File.separator
                + getWorkLogReq.getWorkInstanceId()));
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
                try (BufferedReader bufferedReader = Files.newBufferedReader(logFile.toPath(), StandardCharsets.UTF_8)) {
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

        Configuration flinkConfig = GlobalConfiguration.loadConfiguration();
        flinkConfig.set(DeploymentOptions.TARGET, KubernetesDeploymentTarget.APPLICATION.getName());
        flinkConfig.set(KubernetesConfigOptions.NAMESPACE, "zhiqingyun-space");
        flinkConfig.set(KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT, "zhiqingyun");

        KubernetesClusterClientFactory kubernetesClusterClientFactory = new KubernetesClusterClientFactory();
        try (KubernetesClusterDescriptor clusterDescriptor =
            kubernetesClusterClientFactory.createClusterDescriptor(flinkConfig)) {
            clusterDescriptor.killCluster(stopWorkReq.getAppId());
            return StopWorkRes.builder().build();
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

        Process process = Runtime.getRuntime().exec(new String[] {"kubectl", "logs", "-n", "zhiqingyun-space", "-l",
            "app=" + appId, "--all-containers=true", "--tail=2000"});
        try (
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            BufferedReader errReader =
                new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logBuilder.append(line).append("\n");
            }
            while ((line = errReader.readLine()) != null) {
                logBuilder.append(line).append("\n");
            }
        } finally {
            process.destroy();
        }
        process.waitFor();
    }

    private String tail(String log) {

        if (log.length() <= MAX_LOG_CHARS) {
            return log;
        }
        return log.substring(log.length() - MAX_LOG_CHARS);
    }
}
