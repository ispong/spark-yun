package com.isxcode.star.agent.run.impl;

import com.alibaba.fastjson2.JSON;
import com.isxcode.star.agent.properties.SparkYunAgentProperties;
import com.isxcode.star.agent.run.AgentService;
import com.isxcode.star.api.agent.constants.AgentType;
import com.isxcode.star.api.agent.req.SubmitWorkReq;
import com.isxcode.star.api.work.constants.WorkType;
import com.isxcode.star.backend.api.base.exceptions.IsxAppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.logging.log4j.util.Strings;
import org.apache.spark.launcher.SparkLauncher;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class YarnAgentService implements AgentService {

    private final SparkYunAgentProperties sparkYunAgentProperties;

    @Override
    public String getAgentType() {
        return AgentType.YARN;
    }

    @Override
    public String getMaster(String sparkHomePath) {
        return "yarn";
    }

    @Override
    public SparkLauncher getSparkLauncher(SubmitWorkReq submitWorkReq) {


        SparkLauncher sparkLauncher = new SparkLauncher().setVerbose(false)
            .setMainClass(submitWorkReq.getSparkSubmit().getMainClass()).setDeployMode("cluster")
            .setAppName(submitWorkReq.getSparkSubmit().getAppName() + "-" + submitWorkReq.getWorkType() + "-"
                + submitWorkReq.getWorkId() + "-" + submitWorkReq.getWorkInstanceId())
            .setMaster(getMaster(submitWorkReq.getSparkHomePath()))
            .setAppResource(submitWorkReq.getAgentHomePath() + File.separator + "file" + File.separator
                + submitWorkReq.getSparkSubmit().getAppResource())
            .setSparkHome(submitWorkReq.getAgentHomePath() + File.separator + "spark-min");

        if (WorkType.SPARK_JAR.equals(submitWorkReq.getWorkType())) {
            sparkLauncher
                .setAppName(submitWorkReq.getSparkSubmit().getAppName() + "-" + submitWorkReq.getWorkType() + "-"
                    + submitWorkReq.getWorkId() + "-" + submitWorkReq.getWorkInstanceId())
                .setAppResource(submitWorkReq.getAgentHomePath() + File.separator + "file" + File.separator
                    + submitWorkReq.getSparkSubmit().getAppResource());
        } else if (WorkType.PY_SPARK.equals(submitWorkReq.getWorkType())) {
            sparkLauncher
                .setAppName("zhiqingyun-" + submitWorkReq.getWorkType() + "-" + submitWorkReq.getWorkId() + "-"
                    + submitWorkReq.getWorkInstanceId())
                .setAppResource(submitWorkReq.getAgentHomePath() + File.separator + "works" + File.separator
                    + submitWorkReq.getWorkInstanceId() + ".py");
        } else {
            sparkLauncher
                .setAppName("zhiqingyun-" + submitWorkReq.getWorkType() + "-" + submitWorkReq.getWorkId() + "-"
                    + submitWorkReq.getWorkInstanceId())
                .setAppResource(submitWorkReq.getAgentHomePath() + File.separator + "plugins" + File.separator
                    + submitWorkReq.getSparkSubmit().getAppResource());
        }

        if (!Strings.isEmpty(submitWorkReq.getAgentHomePath())) {
            File[] jarFiles = new File(submitWorkReq.getAgentHomePath() + File.separator + "lib").listFiles();
            if (jarFiles != null) {
                for (File jar : jarFiles) {
                    try {
                        if (jar.getName().contains("hive") || jar.getName().contains("zhiqingyun-agent.jar")) {
                            continue;
                        }
                        sparkLauncher.addJar(jar.toURI().toURL().toString());
                    } catch (MalformedURLException e) {
                        log.error(e.getMessage(), e);
                        throw new IsxAppException("50010", "添加lib中文件异常", e.getMessage());
                    }
                }
            }
        }

        // 引入excel文件
        if (submitWorkReq.getPluginReq().getCsvFilePath() != null) {
            sparkLauncher.addFile(submitWorkReq.getPluginReq().getCsvFilePath());
        }

        // 添加额外依赖
        if (submitWorkReq.getLibConfig() != null) {
            submitWorkReq.getLibConfig().forEach(e -> sparkLauncher
                .addJar(submitWorkReq.getAgentHomePath() + File.separator + "file" + File.separator + e + ".jar"));
        }

        // 添加自定义函数
        if (submitWorkReq.getFuncConfig() != null) {
            submitWorkReq.getFuncConfig().forEach(e -> sparkLauncher.addJar(
                submitWorkReq.getAgentHomePath() + File.separator + "file" + File.separator + e.getFileId() + ".jar"));
        }

        if (WorkType.SPARK_JAR.equals(submitWorkReq.getWorkType())) {
            sparkLauncher.addAppArgs(submitWorkReq.getArgs());
        } else {
            sparkLauncher.addAppArgs(Base64.getEncoder()
                .encodeToString(submitWorkReq.getPluginReq() == null ? submitWorkReq.getArgsStr().getBytes()
                    : JSON.toJSONString(submitWorkReq.getPluginReq()).getBytes()));
        }

        String hiveUsername = submitWorkReq.getSparkSubmit().getConf().get("qing.hive.username");
        if (Strings.isNotEmpty(hiveUsername)) {
            sparkLauncher.setConf("spark.yarn.appMasterEnv.HADOOP_USER_NAME", hiveUsername);
            sparkLauncher.setConf("spark.executorEnv.HADOOP_USER_NAME", hiveUsername);
        }

        // 删除自定义属性
        submitWorkReq.getSparkSubmit().getConf().remove("qing.hive.username");

        // 调整spark.yarn.submit.waitAppCompletion，减少资源消耗
        sparkLauncher.setConf("spark.yarn.submit.waitAppCompletion", "false");
        submitWorkReq.getSparkSubmit().getConf().forEach(sparkLauncher::setConf);

        return sparkLauncher;
    }


    @Override
    public String submitWork(SparkLauncher sparkLauncher) throws Exception {

        Process launch = sparkLauncher.launch();
        InputStream inputStream = launch.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        long timeoutExpiredMs = System.currentTimeMillis() + sparkYunAgentProperties.getSubmitTimeout() * 1000;

        StringBuilder errLog = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            errLog.append(line).append("\n");

            long waitMillis = timeoutExpiredMs - System.currentTimeMillis();
            if (waitMillis <= 0) {
                launch.destroy();
                throw new IsxAppException(errLog.toString());
            }

            String pattern = "Submitted application application_\\d+_\\d+";
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(line);
            if (matcher.find()) {
                return matcher.group().replace("Submitted application ", "");
            }
        }

        try {
            int exitCode = launch.waitFor();
            if (exitCode == 1) {
                throw new IsxAppException(errLog.toString());
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        } finally {
            launch.destroy();
        }

        throw new IsxAppException("无法获取applicationId");
    }

    @Override
    public String getWorkStatus(String appId, String sparkHomePath) throws Exception {

        String getStatusCmdFormat = "yarn application -status %s";

        Process process = Runtime.getRuntime().exec(String.format(getStatusCmdFormat, appId));

        InputStream inputStream = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        StringBuilder errLog = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            errLog.append(line).append("\n");

            String pattern = "Final-State : (\\w+)";
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(line);
            if (matcher.find()) {
                String status = matcher.group(1);
                if ("UNDEFINED".equals(status)) {
                    status = "RUNNING";
                }
                return status;
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode == 1) {
                throw new IsxAppException(errLog.toString());
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }

        throw new IsxAppException("获取状态异常");
    }

    @Override
    public String getStdoutLog(String appId, String sparkHomePath) throws Exception {

        String getLogCmdFormat = "yarn logs -applicationId %s";
        Process process = Runtime.getRuntime().exec(String.format(getLogCmdFormat, appId));

        InputStream inputStream = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        StringBuilder errLog = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            errLog.append(line).append("\n");
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode == 1) {
                throw new IsxAppException(errLog.toString());
            } else {
                Pattern regex = Pattern.compile("LogType:stdout\\s*([\\s\\S]*?)\\s*End of LogType:stdout");
                Matcher matcher = regex.matcher(errLog);
                String log = "";
                while (matcher.find()) {
                    String tmpLog = matcher.group();
                    if (tmpLog.contains("ERROR")) {
                        log = tmpLog;
                        break;
                    }
                    if (tmpLog.length() > log.length()) {
                        log = tmpLog;
                    }
                }
                return log;
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    @Override
    public String getStderrLog(String appId, String sparkHomePath) throws Exception {

        String getLogCmdFormat = "yarn logs -applicationId %s";
        Process process = Runtime.getRuntime().exec(String.format(getLogCmdFormat, appId));

        InputStream inputStream = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        StringBuilder errLog = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            errLog.append(line).append("\n");
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode == 1) {
                throw new IsxAppException(errLog.toString());
            } else {
                Pattern regex = Pattern.compile("LogType:stderr\\s*([\\s\\S]*?)\\s*End of LogType:stderr");
                Matcher matcher = regex.matcher(errLog);
                String log = "";
                while (matcher.find()) {
                    String tmpLog = matcher.group();
                    if (tmpLog.contains("ERROR")) {
                        log = tmpLog;
                        break;
                    }
                    if (tmpLog.length() > log.length()) {
                        log = tmpLog;
                    }
                }
                return log;
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    @Override
    public String getWorkDataStr(String appId, String sparkHomePath) throws Exception {

        String getLogCmdFormat = "yarn logs -applicationId %s";

        Process process = Runtime.getRuntime().exec(String.format(getLogCmdFormat, appId));

        InputStream inputStream = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        StringBuilder errLog = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            errLog.append(line).append("\n");
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode == 1) {
                throw new IsxAppException(errLog.toString());
            } else {
                Pattern regex = Pattern.compile("LogType:spark-yun\\s*([\\s\\S]*?)\\s*End of LogType:spark-yun");
                Matcher matcher = regex.matcher(errLog);
                String log = "";
                while (matcher.find() && Strings.isEmpty(log)) {
                    log = matcher.group().replace("LogType:spark-yun\n", "").replace("\nEnd of LogType:spark-yun", "");
                }
                return log;
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    @Override
    public void stopWork(String appId, String sparkHomePath, String agentHomePath) throws Exception {

        String killAppCmdFormat = "yarn application -kill %s";
        Process process = Runtime.getRuntime().exec(String.format(killAppCmdFormat, appId));

        InputStream inputStream = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        StringBuilder errLog = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            errLog.append(line).append("\n");
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode == 1) {
                throw new IsxAppException(errLog.toString());
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        }
    }

    @Override
    public void getQueue() throws Exception {

        String hadoopHome = System.getenv("HADOOP_HOME");
        if (hadoopHome == null) {
            throw new IsxAppException("HADOOP_HOME not set");
        }

        Configuration conf = new Configuration();
        conf.addResource(hadoopHome + "/etc/hadoop/core-site.xml");
        conf.addResource(hadoopHome + "/etc/hadoop/hdfs-site.xml");
        conf.addResource(hadoopHome + "/etc/hadoop/yarn-site.xml");

        YarnClient yarnClient = YarnClient.createYarnClient();
        yarnClient.init(conf);
        yarnClient.start();

        try {
            List<QueueInfo> queueInfoList = yarnClient.getAllQueues();

            for (QueueInfo queueInfo : queueInfoList) {
                printQueueInfo(queueInfo, "");
            }
        } catch (YarnException | IOException e) {
            log.error(e.getMessage(), e);
            throw new IsxAppException(e.getMessage());
        } finally {
            yarnClient.stop();
        }
    }

    /**
     * 打印队列的详细信息
     *
     * @param queueInfo  队列信息
     * @param parentName 父队列名称，用于递归队列嵌套
     */
    private static void printQueueDetails(QueueInfo queueInfo, String parentName) {
        // 拼接队列全名
        String queueFullName = parentName.isEmpty() ? queueInfo.getQueueName() : parentName + "." + queueInfo.getQueueName();

        System.out.println("-----------------------------------");
        System.out.println("队列全名: " + queueFullName);

        // 打印队列基本信息
        System.out.println("Queue State: " + queueInfo.getQueueState());
        System.out.println("Used Capacity: " + formatResourceWithPercentage(queueInfo.getResourcesUsed(), queueInfo.getUsedCapacity()));
        System.out.println("Configured Capacity: " + formatResource(queueInfo.getQueueResourceQuotas().getConfiguredMinResource()));
        System.out.println("Configured Max Capacity: " + formatResource(queueInfo.getQueueResourceQuotas().getConfiguredMaxResource()));
        System.out.println("Effective Capacity: " + formatResourceWithPercentage(queueInfo.getQueueResourceQuotas().getEffectiveMinResource(), queueInfo.getCapacity()));
        System.out.println("Effective Max Capacity: " + formatResourceWithPercentage(queueInfo.getQueueResourceQuotas().getEffectiveMaxResource(), queueInfo.getMaximumCapacity()));
        System.out.println("Absolute Used Capacity: " + queueInfo.getAbsoluteUsedCapacity() + "%");
        System.out.println("Absolute Configured Capacity: " + queueInfo.getAbsoluteCapacity() + "%");
        System.out.println("Absolute Configured Max Capacity: " + queueInfo.getAbsoluteMaxCapacity() + "%");
        System.out.println("Used Resources: " + formatResource(queueInfo.getResourcesUsed()));

        // 打印应用程序相关配置
        System.out.println("Configured Max Application Master Limit: " + queueInfo.getAMResourceLimit());
        System.out.println("Max Application Master Resources: " + formatResource(queueInfo.getQueueResourceQuotas().getConfiguredAMResourceLimit()));
        System.out.println("Used Application Master Resources: " + formatResource(queueInfo.getAMResourcesUsed()));
        System.out.println("Max Application Master Resources Per User: " + formatResource(queueInfo.getQueueResourceQuotas().getConfiguredAMResourceLimit()));
        System.out.println("Num Schedulable Applications: " + queueInfo.getNumSchedulableApplications());
        System.out.println("Num Non-Schedulable Applications: " + queueInfo.getNumNonSchedulableApplications());
        System.out.println("Num Containers: " + queueInfo.getNumContainers());
        System.out.println("Max Applications: " + queueInfo.getMaxApplications());
        System.out.println("Max Applications Per User: " + queueInfo.getMaxApplicationsPerUser());
        System.out.println("Configured Minimum User Limit Percent: " + queueInfo.getUserLimit() + "%");
        System.out.println("Configured User Limit Factor: " + queueInfo.getUserLimitFactor());

        // 打印队列其他配置
        System.out.println("Accessible Node Labels: " + queueInfo.getAccessibleNodeLabels());
        System.out.println("Ordering Policy: " + queueInfo.getOrderingPolicyInfo());
        System.out.println("Preemption: " + (queueInfo.getPreemptionDisabled() ? "disabled" : "enabled"));
        System.out.println("Intra-queue Preemption: " + (queueInfo.getIntraQueuePreemptionDisabled() ? "disabled" : "enabled"));
        System.out.println("Default Node Label Expression: " + queueInfo.getDefaultNodeLabelExpression());
        System.out.println("Default Application Priority: " + queueInfo.getDefaultApplicationPriority());

        // 递归打印子队列信息
        List<QueueInfo> childQueues = queueInfo.getChildQueues();
        if (childQueues != null && !childQueues.isEmpty()) {
            for (QueueInfo childQueue : childQueues) {
                printQueueDetails(childQueue, queueFullName);
            }
        }
    }

    /**
     * 格式化资源信息
     *
     * @param resource 资源对象
     * @return 格式化后的字符串
     */
    private static String formatResource(org.apache.hadoop.yarn.api.records.Resource resource) {
        if (resource == null) {
            return "unlimited";
        }
        return "<memory:" + resource.getMemorySize() + ", vCores:" + resource.getVirtualCores() + ">";
    }

    /**
     * 格式化资源信息并附带百分比
     *
     * @param resource   资源对象
     * @param percentage 百分比
     * @return 格式化后的字符串
     */
    private static String formatResourceWithPercentage(org.apache.hadoop.yarn.api.records.Resource resource, double percentage) {
        return formatResource(resource) + " (" + percentage + "%)";
    }
}
