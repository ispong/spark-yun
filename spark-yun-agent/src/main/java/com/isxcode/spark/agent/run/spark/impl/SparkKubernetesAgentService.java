package com.isxcode.spark.agent.run.spark.impl;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.isxcode.spark.agent.run.spark.SparkAgentService;
import com.isxcode.spark.agent.run.utils.AgentJavaOptions;
import com.isxcode.spark.agent.run.utils.CommandRunner;
import com.isxcode.spark.agent.run.utils.CommandRunner.CommandResult;
import com.isxcode.spark.api.agent.constants.AgentKubernetes;
import com.isxcode.spark.api.agent.constants.AgentType;
import com.isxcode.spark.api.agent.req.spark.PluginReq;
import com.isxcode.spark.api.agent.req.spark.SubmitWorkReq;
import com.isxcode.spark.api.agent.res.spark.GetWorkInfoRes;
import com.isxcode.spark.api.work.constants.WorkType;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.apache.spark.launcher.SparkLauncher;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SparkKubernetesAgentService implements SparkAgentService {

    private static final Duration KUBECTL_TIMEOUT = Duration.ofSeconds(30);

    private static final String KUBERNETES_NODE_NAME_KEY = "qing.kubernetes.node.name";

    @Override
    public String getAgentType() {
        return AgentType.K8S;
    }

    @Override
    public String getMaster(String sparkHomePath) throws Exception {

        CommandResult clusterInfoResult = CommandRunner.run(Arrays.asList("kubectl", "cluster-info"), KUBECTL_TIMEOUT);
        if (!clusterInfoResult.isSuccess()) {
            throw new Exception(clusterInfoResult.getOutput());
        }

        String[] clusterInfoLines = clusterInfoResult.getStdout().split("\n");
        String result = null;
        for (String infoLine : clusterInfoLines) {
            if (infoLine.contains("https://")) {
                String[] fields = infoLine.split(" ");
                result = fields[fields.length - 1];
                break;
            }
        }

        if (result == null) {
            throw new Exception("No https:// URL found in cluster info.");
        }

        return result.replaceAll("https://", "k8s://").replaceAll("\\u001B\\[[;\\d]*m", "");
    }

    @Override
    public SparkLauncher getSparkLauncher(SubmitWorkReq submitWorkReq) throws Exception {

        // 初始化sparkLauncher
        SparkLauncher sparkLauncher = new SparkLauncher().setVerbose(false).setDeployMode("cluster")
            .setMainClass(submitWorkReq.getSparkSubmit().getMainClass())
            .setMaster(getMaster(submitWorkReq.getSparkHomePath()))
            .setAppResource("local:///opt/spark/examples/jars/" + submitWorkReq.getSparkSubmit().getAppResource())
            .setSparkHome(submitWorkReq.getAgentHomePath() + File.separator + "spark-min");

        // 设置通用配置
        sparkLauncher.setConf("spark.kubernetes.container.image", AgentKubernetes.SPARK_DOCKER_IMAGE);
        sparkLauncher.setConf("spark.kubernetes.namespace", AgentKubernetes.NAMESPACE);
        sparkLauncher.setConf("spark.kubernetes.authenticate.driver.serviceAccountName",
            AgentKubernetes.SERVICE_ACCOUNT_NAME);
        sparkLauncher.setConf("spark.kubernetes.authenticate.executor.serviceAccountName",
            AgentKubernetes.SERVICE_ACCOUNT_NAME);
        sparkLauncher.setConf("spark.kubernetes.driver.volumes.hostPath.jar.mount.path",
            "/opt/spark/examples/jars/" + submitWorkReq.getSparkSubmit().getAppResource());
        sparkLauncher.setConf("spark.kubernetes.executor.volumes.hostPath.jar.mount.path",
            "/opt/spark/examples/jars/" + submitWorkReq.getSparkSubmit().getAppResource());
        sparkLauncher.setConf("spark.kubernetes.container.image.pullPolicy", AgentKubernetes.PULL_POLICY);
        sparkLauncher.setConf("spark.kubernetes.appKillPodDeletionGracePeriod", "600s");

        // 判断是否为自定义任务
        if (WorkType.SPARK_JAR.equals(submitWorkReq.getWorkType())) {
            String appName = submitWorkReq.getSparkSubmit().getAppName() + "-" + submitWorkReq.getWorkType() + "-"
                + submitWorkReq.getWorkId() + "-" + submitWorkReq.getWorkInstanceId();
            sparkLauncher.setAppName(appName);
            String sparkJarPath = submitWorkReq.getAgentHomePath() + File.separator + "file" + File.separator
                + submitWorkReq.getSparkSubmit().getAppResource();
            sparkLauncher.setConf("spark.kubernetes.driver.volumes.hostPath.jar.options.path", sparkJarPath);
            sparkLauncher.setConf("spark.kubernetes.executor.volumes.hostPath.jar.options.path", sparkJarPath);
        } else if (WorkType.PY_SPARK.equals(submitWorkReq.getWorkType())) {
            sparkLauncher.setAppName("zhiqingyun-" + submitWorkReq.getWorkType() + "-" + submitWorkReq.getWorkId() + "-"
                + submitWorkReq.getWorkInstanceId());
            String sparkJarPath = submitWorkReq.getAgentHomePath() + File.separator + "works" + File.separator
                + submitWorkReq.getWorkInstanceId() + ".py";
            sparkLauncher.setConf("spark.kubernetes.driver.volumes.hostPath.jar.options.path", sparkJarPath);
            sparkLauncher.setConf("spark.kubernetes.executor.volumes.hostPath.jar.options.path", sparkJarPath);
        } else {
            String appName = "zhiqingyun-" + submitWorkReq.getWorkType() + "-" + submitWorkReq.getWorkId() + "-"
                + submitWorkReq.getWorkInstanceId();
            sparkLauncher.setAppName(appName);
            String sparkJarPath = submitWorkReq.getAgentHomePath() + File.separator + "plugins" + File.separator
                + submitWorkReq.getSparkSubmit().getAppResource();
            sparkLauncher.setConf("spark.kubernetes.driver.volumes.hostPath.jar.options.path", sparkJarPath);
            sparkLauncher.setConf("spark.kubernetes.executor.volumes.hostPath.jar.options.path", sparkJarPath);
        }

        // 引入至轻云的jar
        if (!Strings.isEmpty(submitWorkReq.getAgentHomePath())) {
            File[] jarFiles = new File(submitWorkReq.getAgentHomePath() + File.separator + "lib").listFiles();
            if (jarFiles != null) {
                for (int i = 0; i < jarFiles.length; i++) {
                    if (jarFiles[i].getName().contains("hive")
                        || jarFiles[i].getName().contains("zhiqingyun-agent.jar")) {
                        continue;
                    }
                    sparkLauncher.addJar("local:///opt/spark/examples/jars/lib/" + jarFiles[i].getName());
                    sparkLauncher.setConf("spark.kubernetes.driver.volumes.hostPath." + i + ".mount.path",
                        "/opt/spark/examples/jars/lib/" + jarFiles[i].getName());
                    sparkLauncher.setConf("spark.kubernetes.driver.volumes.hostPath." + i + ".options.path",
                        jarFiles[i].getPath());
                    sparkLauncher.setConf("spark.kubernetes.executor.volumes.hostPath." + i + ".mount.path",
                        "/opt/spark/examples/jars/lib/" + jarFiles[i].getName());
                    sparkLauncher.setConf("spark.kubernetes.executor.volumes.hostPath." + i + ".options.path",
                        jarFiles[i].getPath());
                }
            }
        }

        // 引入实时计算的jar
        if (WorkType.REAL_WORK.equals(submitWorkReq.getWorkType())) {
            File[] kafkaFiles = new File(submitWorkReq.getSparkHomePath() + File.separator + "jars").listFiles();
            if (kafkaFiles != null) {
                List<String> kafkaFileList =
                    Arrays.asList("spark-sql-kafka-0-10_2.13-3.5.8.jar", "spark-streaming-kafka-0-10_2.13-3.5.8.jar",
                        "spark-token-provider-kafka-0-10_2.13-3.5.8.jar", "commons-pool2-2.11.1.jar",
                        "kafka-clients-3.1.2.jar", "commons-dbutils-1.7.jar", "HikariCP-4.0.3.jar");
                for (int i = 0; i < kafkaFiles.length; i++) {
                    if (kafkaFileList.contains(kafkaFiles[i].getName())) {
                        sparkLauncher.setConf("spark.kubernetes.driver.volumes.hostPath.kafka" + i + ".mount.path",
                            "/opt/spark/jars/" + kafkaFiles[i].getName());
                        sparkLauncher.setConf("spark.kubernetes.driver.volumes.hostPath.kafka" + i + ".options.path",
                            kafkaFiles[i].getPath());
                        sparkLauncher.setConf("spark.kubernetes.executor.volumes.hostPath.kafka" + i + ".mount.path",
                            "/opt/spark/jars/" + kafkaFiles[i].getName());
                        sparkLauncher.setConf("spark.kubernetes.executor.volumes.hostPath.kafka" + i + ".options.path",
                            kafkaFiles[i].getPath());
                    }
                }
            }
        }

        // 引入用户上传的jar
        if (submitWorkReq.getLibConfig() != null) {
            for (int i = 0; i < submitWorkReq.getLibConfig().size(); i++) {
                sparkLauncher.setConf("spark.kubernetes.driver.volumes.hostPath.lib" + i + ".mount.path",
                    "/opt/spark/jars/" + submitWorkReq.getLibConfig().get(i) + ".jar");
                sparkLauncher.setConf("spark.kubernetes.driver.volumes.hostPath.lib" + i + ".options.path",
                    submitWorkReq.getAgentHomePath() + File.separator + "file" + File.separator
                        + submitWorkReq.getLibConfig().get(i) + ".jar");
                sparkLauncher.setConf("spark.kubernetes.executor.volumes.hostPath.lib" + i + ".mount.path",
                    "/opt/spark/jars/" + submitWorkReq.getLibConfig().get(i) + ".jar");
                sparkLauncher.setConf("spark.kubernetes.executor.volumes.hostPath.lib" + i + ".options.path",
                    submitWorkReq.getAgentHomePath() + File.separator + "file" + File.separator
                        + submitWorkReq.getLibConfig().get(i) + ".jar");
            }
        }

        // 引入excel文件
        PluginReq pluginReq = submitWorkReq.getPluginReq();
        String csvFilePath = pluginReq == null ? null : pluginReq.getCsvFilePath();
        if (csvFilePath != null) {
            sparkLauncher.setConf("spark.kubernetes.driver.volumes.hostPath.excel.mount.path", csvFilePath);
            sparkLauncher.setConf("spark.kubernetes.driver.volumes.hostPath.excel.options.path", csvFilePath);
            sparkLauncher.setConf("spark.kubernetes.executor.volumes.hostPath.excel.mount.path", csvFilePath);
            sparkLauncher.setConf("spark.kubernetes.executor.volumes.hostPath.excel.options.path", csvFilePath);
            sparkLauncher.addFile("local://" + csvFilePath);
        }

        // 引入自定义函数
        if (submitWorkReq.getFuncConfig() != null) {
            for (int i = 0; i < submitWorkReq.getFuncConfig().size(); i++) {
                sparkLauncher.setConf("spark.kubernetes.driver.volumes.hostPath.func" + i + ".mount.path",
                    "/opt/spark/jars/" + submitWorkReq.getFuncConfig().get(i).getFileId() + ".jar");
                sparkLauncher.setConf("spark.kubernetes.driver.volumes.hostPath.func" + i + ".options.path",
                    submitWorkReq.getAgentHomePath() + File.separator + "file" + File.separator
                        + submitWorkReq.getFuncConfig().get(i).getFileId() + ".jar");
                sparkLauncher.setConf("spark.kubernetes.executor.volumes.hostPath.func" + i + ".mount.path",
                    "/opt/spark/jars/" + submitWorkReq.getFuncConfig().get(i).getFileId() + ".jar");
                sparkLauncher.setConf("spark.kubernetes.executor.volumes.hostPath.func" + i + ".options.path",
                    submitWorkReq.getAgentHomePath() + File.separator + "file" + File.separator
                        + submitWorkReq.getFuncConfig().get(i).getFileId() + ".jar");
            }
        }

        // 获取sparkConfig配置
        Map<String, String> pluginSparkConfig =
            pluginReq == null || pluginReq.getSparkConfig() == null ? new HashMap<>()
                : new HashMap<>(pluginReq.getSparkConfig());

        // 从sparkConfig中解析出域名映射
        Map<String, String> hostMapping = new HashMap<>();
        if (Strings.isNotEmpty(pluginSparkConfig.get("qing.host1.name"))
            && Strings.isNotEmpty(pluginSparkConfig.get("qing.host1.value"))) {
            hostMapping.put(pluginSparkConfig.get("qing.host1.name"), pluginSparkConfig.get("qing.host1.value"));
        }
        if (Strings.isNotEmpty(pluginSparkConfig.get("qing.host2.name"))
            && Strings.isNotEmpty(pluginSparkConfig.get("qing.host2.value"))) {
            hostMapping.put(pluginSparkConfig.get("qing.host2.name"), pluginSparkConfig.get("qing.host2.value"));
        }
        if (Strings.isNotEmpty(pluginSparkConfig.get("qing.host3.name"))
            && Strings.isNotEmpty(pluginSparkConfig.get("qing.host3.value"))) {
            hostMapping.put(pluginSparkConfig.get("qing.host3.name"), pluginSparkConfig.get("qing.host3.value"));
        }

        String nodeName = resolveKubernetesNodeName(pluginSparkConfig);
        String podTemplate = buildPodTemplate(hostMapping, nodeName);

        // 将文本写到pod-init.yaml中
        String podFileName = submitWorkReq.getWorkInstanceId() + ".yml";
        String podPath = submitWorkReq.getAgentHomePath() + File.separator + "pods" + File.separator + podFileName;
        FileUtil.writeUtf8String(podTemplate, podPath);

        // 配置pod-init.yaml
        sparkLauncher.setConf("spark.kubernetes.driver.podTemplateFile", podPath);
        sparkLauncher.setConf("spark.kubernetes.executor.podTemplateFile", podPath);

        // 获取hive操作人
        String hiveUsername = "";
        if (Strings.isNotEmpty(pluginSparkConfig.get("qing.hive.username"))) {
            hiveUsername = pluginSparkConfig.get("qing.hive.username");
        }

        // sparkLauncher配置操作人
        if (Strings.isNotEmpty(hiveUsername)) {
            sparkLauncher.setConf("spark.kubernetes.driverEnv.SPARK_USER", hiveUsername);
            sparkLauncher.setConf("spark.kubernetes.driverEnv.HADOOP_USER_NAME", hiveUsername);
            sparkLauncher.setConf("spark.executorEnv.SPARK_USER", hiveUsername);
            sparkLauncher.setConf("spark.executorEnv.HADOOP_USER_NAME", hiveUsername);
        }

        // 删除至轻云的自定义参数
        pluginSparkConfig.remove("qing.host1.name");
        pluginSparkConfig.remove("qing.host1.value");
        pluginSparkConfig.remove("qing.host2.name");
        pluginSparkConfig.remove("qing.host2.value");
        pluginSparkConfig.remove("qing.host3.name");
        pluginSparkConfig.remove("qing.host3.value");
        pluginSparkConfig.remove("qing.hive.username");
        pluginSparkConfig.remove(KUBERNETES_NODE_NAME_KEY);
        if (pluginReq != null) {
            pluginReq.setSparkConfig(pluginSparkConfig);
        }

        // 把删除后的sparkConfig，再使用base64压缩一下
        if (WorkType.SPARK_JAR.equals(submitWorkReq.getWorkType())) {
            sparkLauncher.addAppArgs(submitWorkReq.getArgs());
        } else {
            sparkLauncher.addAppArgs(Base64.getEncoder().encodeToString(
                submitWorkReq.getPluginReq() == null ? submitWorkReq.getArgsStr().getBytes(StandardCharsets.UTF_8)
                    : JSON.toJSONString(submitWorkReq.getPluginReq()).getBytes(StandardCharsets.UTF_8)));
        }

        // 把提交的spark配置，塞到sparkLauncher中，必须以spark. 为前缀
        Map<String, String> sparkConfig = submitWorkReq.getSparkSubmit().getConf();
        if (sparkConfig == null) {
            sparkConfig = new HashMap<>();
            submitWorkReq.getSparkSubmit().setConf(sparkConfig);
        }
        sparkConfig.forEach((k, v) -> {
            if (k.startsWith("spark.")) {
                sparkLauncher.setConf(k, v);
            }
        });
        appendJava17ModuleOptions(sparkLauncher, sparkConfig);

        return sparkLauncher;
    }

    private String buildPodTemplate(Map<String, String> hostMapping, String nodeName) {

        StringBuilder podTemplate = new StringBuilder();
        podTemplate.append("apiVersion: v1\n");
        podTemplate.append("kind: Pod\n");
        podTemplate.append("metadata:\n");
        podTemplate.append("  name: pod-template\n");
        podTemplate.append("spec:\n");
        podTemplate.append("  ttlSecondsAfterFinished: 600\n");
        podTemplate.append("  terminationGracePeriodSeconds: 600\n");
        podTemplate.append("  activeDeadlineSeconds: 600\n");
        if (Strings.isNotEmpty(nodeName)) {
            podTemplate.append("  nodeSelector:\n");
            podTemplate.append("    kubernetes.io/hostname: ").append(yamlQuote(nodeName)).append("\n");
        }
        if (!hostMapping.isEmpty()) {
            podTemplate.append("  hostAliases:\n");
            hostMapping.forEach((hostName, ip) -> {
                podTemplate.append("    - ip: ").append(yamlQuote(ip)).append("\n");
                podTemplate.append("      hostnames:\n");
                podTemplate.append("        - ").append(yamlQuote(hostName)).append("\n");
            });
        }
        return podTemplate.toString();
    }

    private String resolveKubernetesNodeName(Map<String, String> pluginSparkConfig) {

        String nodeName = pluginSparkConfig.get(KUBERNETES_NODE_NAME_KEY);
        if (Strings.isEmpty(nodeName)) {
            nodeName = System.getenv("KUBERNETES_NODE_NAME");
        }
        return nodeName;
    }

    private String yamlQuote(String value) {

        return "\"" + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void appendJava17ModuleOptions(SparkLauncher sparkLauncher, Map<String, String> sparkConfig) {

        appendJava17ModuleOptions(sparkLauncher, sparkConfig, "spark.driver.extraJavaOptions");
        appendJava17ModuleOptions(sparkLauncher, sparkConfig, "spark.executor.extraJavaOptions");
    }

    private void appendJava17ModuleOptions(SparkLauncher sparkLauncher, Map<String, String> sparkConfig, String key) {

        sparkLauncher.setConf(key,
            AgentJavaOptions.mergeOptions(sparkConfig.get(key), AgentJavaOptions.SPARK_JAVA_17_MODULE_OPTIONS));
    }

    @Override
    public String submitWork(SparkLauncher sparkLauncher) throws Exception {

        Process launch = sparkLauncher.launch();
        StringBuilder errLog = new StringBuilder();
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(launch.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errLog.append(line).append("\n");
                Matcher matcher = Pattern.compile("pod name: (\\S+)").matcher(line);
                if (matcher.find()) {
                    return matcher.group().replace("pod name: ", "");
                }
            }
        } finally {
            launch.destroy();
        }

        try {
            int exitCode = launch.waitFor();
            if (exitCode == 1) {
                throw new Exception(errLog.toString());
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new Exception(e.getMessage());
        }

        throw new Exception("无法获取podName");
    }

    @Override
    public GetWorkInfoRes getWorkInfo(String podName, String sparkHomePath) throws Exception {

        CommandResult result = CommandRunner
            .run(Arrays.asList("kubectl", "get", "pod", podName, "-n", AgentKubernetes.NAMESPACE), KUBECTL_TIMEOUT);
        String output = result.getOutput();
        if (!result.isSuccess()) {
            if (output.contains("not found")) {
                return GetWorkInfoRes.builder().appId(podName).finalState("KILLED").build();
            }
            throw new Exception(output);
        }
        for (String line : result.getStdout().split("\n")) {
            Matcher matcher = Pattern.compile("\\s+\\d/\\d\\s+(\\w+)").matcher(line);
            if (matcher.find()) {
                return GetWorkInfoRes.builder().appId(podName).finalState(matcher.group(1)).build();
            }
        }

        throw new Exception("获取状态异常");
    }

    @Override
    public String getStderrLog(String appId, String sparkHomePath) throws Exception {

        CommandResult result = readPodLog(appId);
        String logText = result.getOutput();
        if (!result.isSuccess()) {
            throw new Exception(logText);
        }
        if (logText.contains("Error")) {
            return logText;
        }
        Pattern regex = Pattern.compile("LogType:spark-yun\\s*([\\s\\S]*?)\\s*End of LogType:spark-yun");
        Matcher matcher = regex.matcher(logText);
        if (matcher.find()) {
            return logText.replace(matcher.group(), "");
        }
        return logText;
    }

    @Override
    public String getStdoutLog(String appId, String sparkHomePath) throws Exception {

        CommandResult result = readPodLog(appId);
        StringBuilder errLog = new StringBuilder();
        if (!result.isSuccess()) {
            throw new Exception(result.getOutput());
        }
        for (String line : result.getStdout().split("\n")) {
            if (line.contains("累计处理条数")) {
                errLog.append(line).append("\n");
            }
        }
        return errLog.toString();
    }

    @Override
    public String getCustomJarStdoutLog(String appId, String sparkHomePath) throws Exception {
        return "请看运行日志";
    }

    @Override
    public String getWorkDataStr(String appId, String sparkHomePath) throws Exception {

        CommandResult result = readPodLog(appId);
        String logText = result.getStdout();
        if (!result.isSuccess()) {
            throw new Exception(result.getOutput());
        }
        Pattern regex = Pattern.compile("LogType:spark-yun\\s*([\\s\\S]*?)\\s*End of LogType:spark-yun");
        Matcher matcher = regex.matcher(logText);
        String logStr = "";
        while (matcher.find() && Strings.isEmpty(logStr)) {
            logStr = matcher.group().replace("LogType:spark-yun\n", "").replace("\nEnd of LogType:spark-yun", "");
        }
        return logStr;
    }

    @Override
    public void stopWork(String appId, String sparkHomePath, String agentHomePath) throws Exception {

        CommandResult result = CommandRunner
            .run(Arrays.asList("kubectl", "delete", "pod", appId, "-n", AgentKubernetes.NAMESPACE), KUBECTL_TIMEOUT);
        if (!result.isSuccess() && !result.getOutput().contains("not found")) {
            throw new Exception(result.getOutput());
        }
    }

    private CommandResult readPodLog(String appId) throws IOException, InterruptedException {

        return CommandRunner.run(
            Arrays.asList("kubectl", "logs", appId, "-n", AgentKubernetes.NAMESPACE, "--tail=2000"), KUBECTL_TIMEOUT);
    }

    @Override
    public Map<String, String> submitWorkForPySpark(SparkLauncher sparkLauncher) throws Exception {
        return Collections.emptyMap();
    }
}
