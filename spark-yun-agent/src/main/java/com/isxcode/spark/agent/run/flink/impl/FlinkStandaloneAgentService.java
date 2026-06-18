package com.isxcode.spark.agent.run.flink.impl;

import com.alibaba.fastjson2.JSON;
import com.isxcode.spark.agent.run.flink.FlinkAgentService;
import com.isxcode.spark.api.agent.constants.AgentType;
import com.isxcode.spark.api.agent.req.flink.GetWorkInfoReq;
import com.isxcode.spark.api.agent.req.flink.GetWorkLogReq;
import com.isxcode.spark.api.agent.req.flink.StopWorkReq;
import com.isxcode.spark.api.agent.req.flink.SubmitWorkReq;
import com.isxcode.spark.api.agent.res.flink.*;
import com.isxcode.spark.api.work.constants.WorkType;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.client.deployment.StandaloneClusterDescriptor;
import org.apache.flink.client.deployment.StandaloneClusterId;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.client.program.PackagedProgramUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.GlobalConfiguration;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;
import org.apache.flink.runtime.messages.Acknowledge;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class FlinkStandaloneAgentService implements FlinkAgentService {

    private static final String SAVEPOINT_PATH_KEY = "execution.savepoint.path";

    private static final String FLINK_CONFIG_FILE_NAME = "config.yaml";

    private static final String LEGACY_FLINK_CONFIG_FILE_NAME = "flink-conf.yaml";

    private static final int MAX_LOG_CHARS = 30000;

    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

    @Override
    public String getAgentType() {
        return AgentType.StandAlone;
    }

    public Configuration genConfiguration(String flinkHome) {

        String resolvedFlinkHome = resolveFlinkHome(flinkHome);
        String flinkConfDir = resolvedFlinkHome + File.separator + "conf";
        File flinkConfigFile = new File(flinkConfDir, FLINK_CONFIG_FILE_NAME);
        File legacyFlinkConfigFile = new File(flinkConfDir, LEGACY_FLINK_CONFIG_FILE_NAME);

        if (!flinkConfigFile.exists() && !legacyFlinkConfigFile.exists()) {
            throw new IsxAppException("Flink配置文件不存在: " + flinkConfigFile.getAbsolutePath());
        }

        Configuration configuration = GlobalConfiguration.loadConfiguration(flinkConfDir);
        String restAddress = configuration.get(RestOptions.ADDRESS);
        if (isBlank(restAddress) || "0.0.0.0".equals(restAddress) || "::".equals(restAddress)) {
            configuration.set(RestOptions.ADDRESS, "localhost");
        }

        return configuration;
    }

    @Override
    public SubmitWorkRes submitWork(SubmitWorkReq submitWorkReq) throws Exception {

        Configuration configuration = genConfiguration(submitWorkReq.getFlinkHome());
        checkRestAvailable(configuration);

        // 设置作业名称
        configuration.set(PipelineOptions.NAME, submitWorkReq.getFlinkSubmit().getAppName() + "-"
            + submitWorkReq.getWorkType() + "-" + submitWorkReq.getWorkId() + "-" + submitWorkReq.getWorkInstanceId());

        List<URL> userClassPaths = new ArrayList<>();

        // 添加自定义依赖
        if (submitWorkReq.getLibConfig() != null) {
            for (int i = 0; i < submitWorkReq.getLibConfig().size(); i++) {
                userClassPaths.add(requireFile(submitWorkReq.getAgentHomePath() + File.separator + "file"
                    + File.separator + submitWorkReq.getLibConfig().get(i) + ".jar", "Flink依赖包").toURI().toURL());
            }
        }

        // 添加自定义函数
        if (submitWorkReq.getFuncConfig() != null) {
            for (int i = 0; i < submitWorkReq.getFuncConfig().size(); i++) {
                userClassPaths
                    .add(requireFile(submitWorkReq.getAgentHomePath() + File.separator + "file" + File.separator
                        + submitWorkReq.getFuncConfig().get(i).getFileId() + ".jar", "Flink自定义函数包").toURI().toURL());
            }
        }

        if (submitWorkReq.getFlinkSubmit().getConf() != null) {
            submitWorkReq.getFlinkSubmit().getConf().forEach((k, v) -> {
                if (v != null) {
                    configuration.setString(k, String.valueOf(v));
                } else {
                    throw new IllegalArgumentException("Unsupported type for key: " + k + ", value: " + v);
                }
            });
        }

        PackagedProgram program;
        if (WorkType.FLINK_JAR.equals(submitWorkReq.getWorkType())) {
            File jarFile = requireFile(submitWorkReq.getAgentHomePath() + File.separator + "file" + File.separator
                + submitWorkReq.getFlinkSubmit().getAppResource(), "Flink作业Jar");
            PackagedProgram.Builder builder = PackagedProgram.newBuilder().setJarFile(jarFile)
                .setEntryPointClassName(submitWorkReq.getFlinkSubmit().getEntryClass()).setConfiguration(configuration)
                .setArguments(submitWorkReq.getPluginReq().getArgs()).setUserClassPaths(userClassPaths);
            if (configuration.getString(SAVEPOINT_PATH_KEY, null) != null) {
                program =
                    builder
                        .setSavepointRestoreSettings(
                            SavepointRestoreSettings.forPath(configuration.getString(SAVEPOINT_PATH_KEY, null)))
                        .build();
            } else {
                program = builder.build();
            }
        } else {
            File pluginFile = requireFile(submitWorkReq.getAgentHomePath() + File.separator + "plugins" + File.separator
                + submitWorkReq.getFlinkSubmit().getAppResource(), "Flink插件Jar");
            PackagedProgram.Builder builder = PackagedProgram.newBuilder().setJarFile(pluginFile)
                .setEntryPointClassName(submitWorkReq.getFlinkSubmit().getEntryClass()).setConfiguration(configuration)
                .setArguments(Base64.getEncoder()
                    .encodeToString(JSON.toJSONString(submitWorkReq.getPluginReq()).getBytes(StandardCharsets.UTF_8)))
                .setUserClassPaths(userClassPaths);
            if (configuration.getString(SAVEPOINT_PATH_KEY, null) != null) {
                program =
                    builder
                        .setSavepointRestoreSettings(
                            SavepointRestoreSettings.forPath(configuration.getString(SAVEPOINT_PATH_KEY, null)))
                        .build();
            } else {
                program = builder.build();
            }
        }

        try (StandaloneClusterDescriptor standaloneClusterDescriptor = new StandaloneClusterDescriptor(configuration);
            ClusterClient<StandaloneClusterId> clusterClient =
                standaloneClusterDescriptor.retrieve(StandaloneClusterId.getInstance()).getClusterClient()) {

            JobGraph jobGraph = PackagedProgramUtils.createJobGraph(program, configuration, 1, false);
            JobID jobID = clusterClient.submitJob(jobGraph).get();
            return SubmitWorkRes.builder().appId(jobID.toHexString()).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception("提交Flink Local作业失败: " + getRootMessage(e), e);
        }
    }

    @Override
    public GetWorkInfoRes getWorkInfo(GetWorkInfoReq getWorkInfoReq) throws Exception {

        Configuration configuration = genConfiguration(getWorkInfoReq.getFlinkHome());

        try (StandaloneClusterDescriptor standaloneClusterDescriptor = new StandaloneClusterDescriptor(configuration);
            ClusterClient<StandaloneClusterId> clusterClient =
                standaloneClusterDescriptor.retrieve(StandaloneClusterId.getInstance()).getClusterClient()) {

            CompletableFuture<JobStatus> jobStatus =
                clusterClient.getJobStatus(JobID.fromHexString(getWorkInfoReq.getAppId()));

            return GetWorkInfoRes.builder().appId(getWorkInfoReq.getAppId()).finalState(jobStatus.get().name()).build();
        }
    }

    @Override
    public GetWorkLogRes getWorkLog(GetWorkLogReq getWorkLogReq) throws Exception {

        Configuration configuration = genConfiguration(getWorkLogReq.getFlinkHome());
        String restUrl = getRestUrl(configuration);
        StringBuilder logBuilder = new StringBuilder();

        // 判断作业是否成功
        String status;
        try (StandaloneClusterDescriptor standaloneClusterDescriptor = new StandaloneClusterDescriptor(configuration);
            ClusterClient<StandaloneClusterId> clusterClient =
                standaloneClusterDescriptor.retrieve(StandaloneClusterId.getInstance()).getClusterClient()) {

            CompletableFuture<JobStatus> jobStatus =
                clusterClient.getJobStatus(JobID.fromHexString(getWorkLogReq.getAppId()));
            status = jobStatus.get().name();
        }

        appendJobExceptionLog(restUrl, getWorkLogReq.getAppId(), logBuilder);
        appendRestLogSection(restUrl + "/jobmanager/log", "JobManager Log", logBuilder);
        appendRestLogSection(restUrl + "/jobmanager/stdout", "JobManager Stdout", logBuilder);
        appendTaskManagerLogs(restUrl, logBuilder);

        if (logBuilder.length() == 0) {
            return GetWorkLogRes.builder().log("未获取到Flink日志，当前作业状态: " + status + "，请检查Flink Web日志接口是否可访问").build();
        }

        return GetWorkLogRes.builder().log(tailLog(logBuilder.toString())).build();
    }

    @Override
    public StopWorkRes stopWork(StopWorkReq stopWorkReq) throws Exception {

        Configuration configuration = genConfiguration(stopWorkReq.getFlinkHome());

        try (StandaloneClusterDescriptor standaloneClusterDescriptor = new StandaloneClusterDescriptor(configuration);
            ClusterClient<StandaloneClusterId> clusterClient =
                standaloneClusterDescriptor.retrieve(StandaloneClusterId.getInstance()).getClusterClient()) {

            CompletableFuture<Acknowledge> cancel = clusterClient.cancel(JobID.fromHexString(stopWorkReq.getAppId()));
            return StopWorkRes.builder().requestId(cancel.toString()).build();
        }
    }

    private String resolveFlinkHome(String flinkHome) {

        String resolvedFlinkHome = !Strings.isEmpty(flinkHome) ? flinkHome : System.getenv("FLINK_HOME");
        if (isBlank(resolvedFlinkHome)) {
            throw new IsxAppException("Flink Home未配置，请在计算集群节点中配置Flink安装目录或设置FLINK_HOME");
        }

        File flinkHomeFile = new File(resolvedFlinkHome);
        if (!flinkHomeFile.exists() || !flinkHomeFile.isDirectory()) {
            throw new IsxAppException("Flink Home不存在: " + resolvedFlinkHome);
        }

        return flinkHomeFile.getAbsolutePath();
    }

    private File requireFile(String filePath, String fileName) {

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IsxAppException(fileName + "不存在: " + file.getAbsolutePath());
        }

        return file;
    }

    private void checkRestAvailable(Configuration configuration) {

        String restUrl = getRestUrl(configuration);
        try {
            ResponseEntity<String> responseEntity = REST_TEMPLATE.getForEntity(restUrl + "/overview", String.class);
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                throw new IsxAppException("Flink Local REST服务不可用: " + restUrl);
            }
        } catch (Exception e) {
            throw new IsxAppException("Flink Local REST服务不可用: " + restUrl + "，请确认Flink集群已启动，原因: " + getRootMessage(e));
        }
    }

    private String getRestUrl(Configuration configuration) {

        return "http://" + configuration.get(RestOptions.ADDRESS) + ":" + configuration.get(RestOptions.PORT);
    }

    private void appendJobExceptionLog(String restUrl, String appId, StringBuilder logBuilder) {

        try {
            ResponseEntity<FlinkRestExceptionRes> exceptionResult =
                REST_TEMPLATE.getForEntity(restUrl + "/jobs/" + appId + "/exceptions", FlinkRestExceptionRes.class);
            if (exceptionResult.getStatusCode().is2xxSuccessful() && exceptionResult.getBody() != null
                && !isBlank(exceptionResult.getBody().getRootException())) {
                appendSection(logBuilder, "Job Exception", exceptionResult.getBody().getRootException());
            }
        } catch (Exception e) {
            log.warn("获取Flink异常日志失败: {}", getRootMessage(e));
        }
    }

    private void appendTaskManagerLogs(String restUrl, StringBuilder logBuilder) {

        try {
            ResponseEntity<FlinkGetTaskManagerRes> responseEntity =
                REST_TEMPLATE.getForEntity(restUrl + "/taskmanagers", FlinkGetTaskManagerRes.class);
            if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null
                || responseEntity.getBody().getTaskManagers() == null
                || responseEntity.getBody().getTaskManagers().isEmpty()) {
                appendSection(logBuilder, "TaskManager", "未获取到TaskManager列表");
                return;
            }

            for (FlinkTaskManagerRes taskManager : responseEntity.getBody().getTaskManagers()) {
                if (taskManager == null || isBlank(taskManager.getId())) {
                    continue;
                }
                appendRestLogSection(restUrl + "/taskmanagers/" + taskManager.getId() + "/log",
                    "TaskManager " + taskManager.getId() + " Log", logBuilder);
                appendRestLogSection(restUrl + "/taskmanagers/" + taskManager.getId() + "/stdout",
                    "TaskManager " + taskManager.getId() + " Stdout", logBuilder);
            }
        } catch (Exception e) {
            appendSection(logBuilder, "TaskManager", "获取TaskManager日志失败: " + getRootMessage(e));
        }
    }

    private void appendRestLogSection(String url, String title, StringBuilder logBuilder) {

        try {
            ResponseEntity<String> responseEntity = REST_TEMPLATE.getForEntity(url, String.class);
            if (responseEntity.getStatusCode().is2xxSuccessful() && !isBlank(responseEntity.getBody())) {
                appendSection(logBuilder, title, responseEntity.getBody());
            }
        } catch (Exception e) {
            log.warn("获取Flink日志失败: {}, {}", title, getRootMessage(e));
        }
    }

    private void appendSection(StringBuilder logBuilder, String title, String content) {

        if (isBlank(content)) {
            return;
        }

        logBuilder.append("\n================ ").append(title).append(" ================\n")
            .append(tailLog(content.trim())).append('\n');
    }

    private String tailLog(String logContent) {

        if (logContent == null || logContent.length() <= MAX_LOG_CHARS) {
            return logContent;
        }

        return "日志过长，仅展示最后" + MAX_LOG_CHARS + "个字符\n" + logContent.substring(logContent.length() - MAX_LOG_CHARS);
    }

    private String getRootMessage(Throwable throwable) {

        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        String message = root.getMessage();
        return isBlank(message) ? root.getClass().getName() : message;
    }

    private boolean isBlank(String value) {

        return value == null || value.trim().isEmpty();
    }
}
