package com.isxcode.star.modules.work.run.impl;

import com.alibaba.fastjson.JSON;
import com.isxcode.star.api.agent.constants.AgentUrl;
import com.isxcode.star.api.agent.pojos.req.*;
import com.isxcode.star.api.agent.pojos.res.GetWorkStderrLogRes;
import com.isxcode.star.api.api.constants.PathConstants;
import com.isxcode.star.api.cluster.constants.ClusterNodeStatus;
import com.isxcode.star.api.cluster.pojos.dto.ScpFileEngineNodeDto;
import com.isxcode.star.api.work.constants.WorkLog;
import com.isxcode.star.api.work.constants.WorkType;
import com.isxcode.star.api.work.exceptions.WorkRunException;
import com.isxcode.star.api.work.pojos.dto.ClusterConfig;
import com.isxcode.star.api.work.pojos.dto.DatasourceConfig;
import com.isxcode.star.api.work.pojos.res.RunWorkRes;
import com.isxcode.star.backend.api.base.exceptions.IsxAppException;
import com.isxcode.star.backend.api.base.pojos.BaseResponse;
import com.isxcode.star.backend.api.base.properties.IsxAppProperties;
import com.isxcode.star.common.locker.Locker;
import com.isxcode.star.common.utils.AesUtils;
import com.isxcode.star.common.utils.http.HttpUrlUtils;
import com.isxcode.star.common.utils.http.HttpUtils;
import com.isxcode.star.common.utils.path.PathUtils;
import com.isxcode.star.modules.alarm.service.AlarmService;
import com.isxcode.star.modules.cluster.entity.ClusterEntity;
import com.isxcode.star.modules.cluster.entity.ClusterNodeEntity;
import com.isxcode.star.modules.cluster.mapper.ClusterNodeMapper;
import com.isxcode.star.modules.cluster.repository.ClusterNodeRepository;
import com.isxcode.star.modules.cluster.repository.ClusterRepository;
import com.isxcode.star.modules.datasource.entity.DatasourceEntity;
import com.isxcode.star.modules.datasource.service.DatasourceService;
import com.isxcode.star.modules.file.entity.FileEntity;
import com.isxcode.star.modules.file.repository.FileRepository;
import com.isxcode.star.modules.func.entity.FuncEntity;
import com.isxcode.star.modules.func.mapper.FuncMapper;
import com.isxcode.star.modules.func.repository.FuncRepository;
import com.isxcode.star.modules.work.entity.WorkConfigEntity;
import com.isxcode.star.modules.work.entity.WorkEntity;
import com.isxcode.star.modules.work.entity.WorkInstanceEntity;
import com.isxcode.star.modules.work.repository.WorkConfigRepository;
import com.isxcode.star.modules.work.repository.WorkInstanceRepository;
import com.isxcode.star.modules.work.repository.WorkRepository;
import com.isxcode.star.modules.work.run.WorkExecutor;
import com.isxcode.star.modules.work.run.WorkRunContext;
import com.isxcode.star.modules.work.sql.SqlCommentService;
import com.isxcode.star.modules.work.sql.SqlFunctionService;
import com.isxcode.star.modules.work.sql.SqlValueService;
import com.isxcode.star.modules.workflow.repository.WorkflowInstanceRepository;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static com.isxcode.star.common.config.CommonConfig.TENANT_ID;
import static com.isxcode.star.common.utils.ssh.SshUtils.scpJar;

@Service
@Slf4j
public class SyncWorkExecutor extends WorkExecutor {

    private final WorkInstanceRepository workInstanceRepository;

    private final ClusterRepository clusterRepository;

    private final ClusterNodeRepository clusterNodeRepository;

    private final WorkRepository workRepository;

    private final WorkConfigRepository workConfigRepository;

    private final Locker locker;

    private final HttpUrlUtils httpUrlUtils;

    private final AesUtils aesUtils;

    private final ClusterNodeMapper clusterNodeMapper;

    private final DatasourceService datasourceService;

    private final IsxAppProperties isxAppProperties;

    private final FuncRepository funcRepository;

    private final FuncMapper funcMapper;

    private final FileRepository fileRepository;

    private final SqlCommentService sqlCommentService;

    private final SqlValueService sqlValueService;

    private final SqlFunctionService sqlFunctionService;

    public SyncWorkExecutor(WorkInstanceRepository workInstanceRepository, ClusterRepository clusterRepository,
        ClusterNodeRepository clusterNodeRepository, WorkflowInstanceRepository workflowInstanceRepository,
        WorkRepository workRepository, WorkConfigRepository workConfigRepository, Locker locker,
        HttpUrlUtils httpUrlUtils, AesUtils aesUtils, ClusterNodeMapper clusterNodeMapper,
        DatasourceService datasourceService, IsxAppProperties isxAppProperties, FuncRepository funcRepository,
        FuncMapper funcMapper, FileRepository fileRepository, SqlCommentService sqlCommentService,
        SqlValueService sqlValueService, SqlFunctionService sqlFunctionService, AlarmService alarmService) {

        super(workInstanceRepository, workflowInstanceRepository, alarmService);
        this.workInstanceRepository = workInstanceRepository;
        this.clusterRepository = clusterRepository;
        this.clusterNodeRepository = clusterNodeRepository;
        this.workRepository = workRepository;
        this.workConfigRepository = workConfigRepository;
        this.locker = locker;
        this.httpUrlUtils = httpUrlUtils;
        this.aesUtils = aesUtils;
        this.clusterNodeMapper = clusterNodeMapper;
        this.datasourceService = datasourceService;
        this.isxAppProperties = isxAppProperties;
        this.funcRepository = funcRepository;
        this.funcMapper = funcMapper;
        this.fileRepository = fileRepository;
        this.sqlCommentService = sqlCommentService;
        this.sqlValueService = sqlValueService;
        this.sqlFunctionService = sqlFunctionService;
    }

    @Override
    public String getWorkType() {
        return WorkType.DATA_SYNC_JDBC;
    }

    @Override
    protected void execute(WorkRunContext workRunContext, WorkInstanceEntity workInstance) {

        // 将线程存到Map
        WORK_THREAD.put(workInstance.getId(), Thread.currentThread());

        // 获取日志构造器
        StringBuilder logBuilder = workRunContext.getLogBuilder();

        // 检测计算集群是否配置
        logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("开始申请资源 \n");
        if (Strings.isEmpty(workRunContext.getClusterConfig().getClusterId())) {
            throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "申请资源失败 : 计算引擎不存在  \n");
        }

        // 检查计算集群是否存在
        Optional<ClusterEntity> calculateEngineEntityOptional =
            clusterRepository.findById(workRunContext.getClusterConfig().getClusterId());
        if (!calculateEngineEntityOptional.isPresent()) {
            throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "申请资源失败 : 计算引擎不存在  \n");
        }

        // 检测集群中是否有合法节点
        List<ClusterNodeEntity> allEngineNodes = clusterNodeRepository
            .findAllByClusterIdAndStatus(calculateEngineEntityOptional.get().getId(), ClusterNodeStatus.RUNNING);
        if (allEngineNodes.isEmpty()) {
            throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "申请资源失败 : 集群不存在可用节点，请切换一个集群  \n");
        }

        // 检查分区键是否为空
        if (Strings.isEmpty(workRunContext.getSyncWorkConfig().getPartitionColumn())) {
            throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "检查作业失败 : 分区键为空  \n");
        }

        // 检测用户是否配置映射关系
        if (workRunContext.getSyncWorkConfig().getColumnMap().isEmpty()) {
            throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "检查作业失败 : 请配置字段映射关系  \n");
        }

        // 节点选择随机数
        ClusterNodeEntity engineNode = allEngineNodes.get(new Random().nextInt(allEngineNodes.size()));
        logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("申请资源完成，激活节点:【")
            .append(engineNode.getName()).append("】\n");
        logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("检测运行环境完成  \n");
        workInstance = updateInstance(workInstance, logBuilder);

        // 开始构建作业
        logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("开始构建作业  \n");
        SubmitWorkReq executeReq = new SubmitWorkReq();
        executeReq.setWorkId(workRunContext.getWorkId());
        executeReq.setWorkType(WorkType.DATA_SYNC_JDBC);
        executeReq.setWorkInstanceId(workInstance.getId());

        // 封装来源Datasource的信息
        DatasourceEntity sourceDatasource =
            datasourceService.getDatasource(workRunContext.getSyncWorkConfig().getSourceDBId());
        DatasourceConfig sourceConfig =
            DatasourceConfig.builder().driver(datasourceService.getDriverClass(sourceDatasource.getDbType()))
                .url(sourceDatasource.getJdbcUrl()).dbTable(workRunContext.getSyncWorkConfig().getSourceTable())
                .user(sourceDatasource.getUsername()).password(aesUtils.decrypt(sourceDatasource.getPasswd())).build();
        workRunContext.getSyncWorkConfig().setSourceDatabase(sourceConfig);

        // 封装去向Datasource的信息
        DatasourceEntity targetDatasource =
            datasourceService.getDatasource(workRunContext.getSyncWorkConfig().getTargetDBId());
        DatasourceConfig targetConfig =
            DatasourceConfig.builder().driver(datasourceService.getDriverClass(targetDatasource.getDbType()))
                .url(targetDatasource.getJdbcUrl()).dbTable(workRunContext.getSyncWorkConfig().getTargetTable())
                .user(targetDatasource.getUsername()).password(aesUtils.decrypt(targetDatasource.getPasswd())).build();
        workRunContext.getSyncWorkConfig().setTargetDatabase(targetConfig);

        // 开始构造SparkSubmit
        SparkSubmit sparkSubmit = SparkSubmit.builder().verbose(true)
            .mainClass("com.isxcode.star.plugin.dataSync.jdbc.Execute").appResource("spark-data-sync-jdbc-plugin.jar")
            .conf(genSparkSubmitConfig(workRunContext.getClusterConfig().getSparkConfig())).build();

        // 过滤条件支持系统参数和函数解析
        if (!Strings.isEmpty(workRunContext.getSyncWorkConfig().getQueryCondition())) {

            // 去掉sql中的注释
            String sqlNoComment =
                sqlCommentService.removeSqlComment(workRunContext.getSyncWorkConfig().getQueryCondition());

            // 翻译sql中的系统变量
            String parseValueSql = sqlValueService.parseSqlValue(sqlNoComment);

            // 翻译sql中的系统函数
            String conditionScript = sqlFunctionService.parseSqlFunction(parseValueSql);

            logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("过滤条件:  \n")
                .append(conditionScript).append("\n");
            workInstance = updateInstance(workInstance, logBuilder);
            workRunContext.getSyncWorkConfig().setQueryCondition(conditionScript);
        }

        // 开始构造PluginReq
        PluginReq pluginReq = PluginReq.builder().syncWorkConfig(workRunContext.getSyncWorkConfig())
            .sparkConfig(genSparkConfig(workRunContext.getClusterConfig().getSparkConfig()))
            .syncRule(workRunContext.getSyncRule()).build();

        // 导入自定义函数
        ScpFileEngineNodeDto scpFileEngineNodeDto =
            clusterNodeMapper.engineNodeEntityToScpFileEngineNodeDto(engineNode);
        scpFileEngineNodeDto.setPasswd(aesUtils.decrypt(scpFileEngineNodeDto.getPasswd()));
        String fileDir = PathUtils.parseProjectPath(isxAppProperties.getResourcesPath()) + File.separator + "file"
            + File.separator + TENANT_ID.get();
        if (workRunContext.getFuncConfig() != null) {
            List<FuncEntity> allFunc = funcRepository.findAllById(workRunContext.getFuncConfig());
            allFunc.forEach(e -> {
                try {
                    scpJar(scpFileEngineNodeDto, fileDir + File.separator + e.getFileId(),
                        engineNode.getAgentHomePath() + "/zhiqingyun-agent/file/" + e.getFileId() + ".jar");
                } catch (JSchException | SftpException | InterruptedException | IOException ex) {
                    throw new WorkRunException(
                        LocalDateTime.now() + WorkLog.ERROR_INFO + " : jar文件上传失败," + ex.getMessage() + "\n");
                }
            });
            pluginReq.setFuncInfoList(funcMapper.funcEntityListToFuncInfoList(allFunc));
            executeReq.setFuncConfig(funcMapper.funcEntityListToFuncInfoList(allFunc));
        }

        // 上传依赖到制定节点路径
        if (workRunContext.getLibConfig() != null) {
            List<FileEntity> libFile = fileRepository.findAllById(workRunContext.getLibConfig());
            libFile.forEach(e -> {
                try {
                    scpJar(scpFileEngineNodeDto, fileDir + File.separator + e.getId(),
                        engineNode.getAgentHomePath() + File.separator + "zhiqingyun-agent" + File.separator + "file"
                            + File.separator + e.getId() + ".jar");
                } catch (JSchException | SftpException | InterruptedException | IOException ex) {
                    throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "jar文件上传失败\n");
                }
            });
            executeReq.setLibConfig(workRunContext.getLibConfig());
        }

        // 开始构造executeReq
        executeReq.setSparkSubmit(sparkSubmit);
        executeReq.setPluginReq(pluginReq);
        executeReq.setAgentHomePath(engineNode.getAgentHomePath() + "/" + PathConstants.AGENT_PATH_NAME);
        executeReq.setSparkHomePath(engineNode.getSparkHomePath());
        executeReq.setClusterType(calculateEngineEntityOptional.get().getClusterType());

        // 构建作业完成，并打印作业配置信息
        logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("构建作业完成 \n");
        workRunContext.getClusterConfig().getSparkConfig().forEach((k, v) -> logBuilder.append(LocalDateTime.now())
            .append(WorkLog.SUCCESS_INFO).append(k).append(":").append(v).append(" \n"));
        logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("开始提交作业  \n");
        workInstance = updateInstance(workInstance, logBuilder);

        // 开始提交作业
        BaseResponse<?> baseResponse;

        // 加锁，必须等待作业提交成功后才能中止
        Integer lock = locker.lock("REQUEST_" + workInstance.getId());
        RunWorkRes submitWorkRes;
        try {
            baseResponse = HttpUtils.doPost(
                httpUrlUtils.genHttpUrl(engineNode.getHost(), engineNode.getAgentPort(), AgentUrl.SUBMIT_WORK_URL),
                executeReq, BaseResponse.class);
            log.debug("获取远程提交作业日志:{}", baseResponse.toString());
            if (!String.valueOf(HttpStatus.OK.value()).equals(baseResponse.getCode())) {
                throw new WorkRunException(
                    LocalDateTime.now() + WorkLog.ERROR_INFO + "提交作业失败 : " + baseResponse.getMsg() + "\n");
            }
            // 解析返回对象,获取appId
            if (baseResponse.getData() == null) {
                throw new WorkRunException(
                    LocalDateTime.now() + WorkLog.ERROR_INFO + "提交作业失败 : " + baseResponse.getMsg() + "\n");
            }
            submitWorkRes = JSON.parseObject(JSON.toJSONString(baseResponse.getData()), RunWorkRes.class);
            logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("提交作业成功 : ")
                .append(submitWorkRes.getAppId()).append("\n");
            workInstance.setSparkStarRes(JSON.toJSONString(submitWorkRes));
            workInstance = updateInstance(workInstance, logBuilder);
        } catch (ResourceAccessException e) {
            throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "提交作业失败 : " + e.getMessage() + "\n");
        } catch (HttpServerErrorException e1) {
            if (HttpStatus.BAD_GATEWAY.value() == e1.getRawStatusCode()) {
                throw new WorkRunException(
                    LocalDateTime.now() + WorkLog.ERROR_INFO + "提交作业失败 : 无法访问节点服务器,请检查服务器防火墙或者计算集群\n");
            }
            throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "提交作业失败 : " + e1.getMessage() + "\n");
        } finally {
            locker.unlock(lock);
        }

        // 提交作业成功后，开始循环判断状态
        while (true) {

            // 获取作业状态并保存
            GetWorkStatusReq getWorkStatusReq = GetWorkStatusReq.builder().appId(submitWorkRes.getAppId())
                .clusterType(calculateEngineEntityOptional.get().getClusterType())
                .sparkHomePath(executeReq.getSparkHomePath()).build();
            baseResponse = HttpUtils.doPost(
                httpUrlUtils.genHttpUrl(engineNode.getHost(), engineNode.getAgentPort(), AgentUrl.GET_WORK_STATUS_URL),
                getWorkStatusReq, BaseResponse.class);
            log.debug("获取远程获取状态日志:{}", baseResponse.toString());

            if (!String.valueOf(HttpStatus.OK.value()).equals(baseResponse.getCode())) {
                throw new WorkRunException(
                    LocalDateTime.now() + WorkLog.ERROR_INFO + "获取作业状态异常 : " + baseResponse.getMsg() + "\n");
            }

            // 解析返回状态，并保存
            RunWorkRes workStatusRes = JSON.parseObject(JSON.toJSONString(baseResponse.getData()), RunWorkRes.class);
            workInstance.setSparkStarRes(JSON.toJSONString(workStatusRes));

            // 如果上一行是RUNNING，状态也是RUNNING，不更新日志，否则日志太长了
            String[] split = logBuilder.toString().split("\n");
            List<String> logList = Arrays.asList(split);
            if (!(logList.get(logList.size() - 1).toUpperCase().contains("RUNNING")
                && "RUNNING".equalsIgnoreCase(workStatusRes.getAppStatus()))) {
                logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("运行状态:")
                    .append(workStatusRes.getAppStatus()).append("\n");
            }

            workInstance = updateInstance(workInstance, logBuilder);

            // 如果状态是运行中，更新日志，继续执行
            List<String> runningStatus = Arrays.asList("RUNNING", "UNDEFINED", "SUBMITTED", "CONTAINERCREATING");
            if (runningStatus.contains(workStatusRes.getAppStatus().toUpperCase())) {
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    throw new WorkRunException(
                        LocalDateTime.now() + WorkLog.ERROR_INFO + "睡眠线程异常 : " + e.getMessage() + "\n");
                }
            } else {
                // 运行结束逻辑

                // 如果是中止，直接退出
                if ("KILLED".equals(workStatusRes.getAppStatus().toUpperCase())
                    || "TERMINATING".equals(workStatusRes.getAppStatus().toUpperCase())) {
                    throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "作业运行中止" + "\n");
                }

                // 获取日志并保存
                GetWorkStderrLogReq getWorkStderrLogReq = GetWorkStderrLogReq.builder().appId(submitWorkRes.getAppId())
                    .clusterType(calculateEngineEntityOptional.get().getClusterType())
                    .sparkHomePath(executeReq.getSparkHomePath()).build();
                baseResponse = HttpUtils.doPost(httpUrlUtils.genHttpUrl(engineNode.getHost(), engineNode.getAgentPort(),
                    AgentUrl.GET_WORK_STDERR_LOG_URL), getWorkStderrLogReq, BaseResponse.class);
                log.debug("获取远程返回日志:{}", baseResponse.toString());

                if (!String.valueOf(HttpStatus.OK.value()).equals(baseResponse.getCode())) {
                    throw new WorkRunException(
                        LocalDateTime.now() + WorkLog.ERROR_INFO + "获取作业日志异常 : " + baseResponse.getMsg() + "\n");
                }

                // 解析日志并保存
                GetWorkStderrLogRes yagGetLogRes =
                    JSON.parseObject(JSON.toJSONString(baseResponse.getData()), GetWorkStderrLogRes.class);
                logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("日志保存成功 \n");
                if (yagGetLogRes != null) {
                    workInstance.setYarnLog(yagGetLogRes.getLog());
                }
                updateInstance(workInstance, logBuilder);

                // 如果运行成功，则保存返回数据
                List<String> successStatus = Arrays.asList("FINISHED", "SUCCEEDED", "COMPLETED");
                if (!successStatus.contains(workStatusRes.getAppStatus().toUpperCase())) {
                    // 任务运行错误
                    throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "任务运行异常" + "\n");
                }

                // 运行结束，则退出死循环
                break;
            }
        }
    }

    @Override
    protected void abort(WorkInstanceEntity workInstance) {

        // 判断作业有没有提交成功
        locker.lock("REQUEST_" + workInstance.getId());
        try {
            workInstance = workInstanceRepository.findById(workInstance.getId()).get();
            if (!Strings.isEmpty(workInstance.getSparkStarRes())) {
                RunWorkRes wokRunWorkRes = JSON.parseObject(workInstance.getSparkStarRes(), RunWorkRes.class);
                if (!Strings.isEmpty(wokRunWorkRes.getAppId())) {
                    // 关闭远程线程
                    WorkEntity work = workRepository.findById(workInstance.getWorkId()).get();
                    WorkConfigEntity workConfig = workConfigRepository.findById(work.getConfigId()).get();
                    ClusterConfig clusterConfig = JSON.parseObject(workConfig.getClusterConfig(), ClusterConfig.class);
                    List<ClusterNodeEntity> allEngineNodes = clusterNodeRepository
                        .findAllByClusterIdAndStatus(clusterConfig.getClusterId(), ClusterNodeStatus.RUNNING);
                    if (allEngineNodes.isEmpty()) {
                        throw new WorkRunException(
                            LocalDateTime.now() + WorkLog.ERROR_INFO + "申请资源失败 : 集群不存在可用节点，请切换一个集群  \n");
                    }
                    ClusterEntity cluster = clusterRepository.findById(clusterConfig.getClusterId()).get();

                    // 节点选择随机数
                    ClusterNodeEntity engineNode = allEngineNodes.get(new Random().nextInt(allEngineNodes.size()));

                    StopWorkReq stopWorkReq = StopWorkReq.builder().appId(wokRunWorkRes.getAppId())
                        .clusterType(cluster.getClusterType()).sparkHomePath(engineNode.getSparkHomePath())
                        .agentHomePath(engineNode.getAgentHomePath()).build();
                    BaseResponse<?> baseResponse = HttpUtils.doPost(httpUrlUtils.genHttpUrl(engineNode.getHost(),
                        engineNode.getAgentPort(), AgentUrl.STOP_WORK_URL), stopWorkReq, BaseResponse.class);

                    if (!String.valueOf(HttpStatus.OK.value()).equals(baseResponse.getCode())) {
                        throw new IsxAppException(baseResponse.getCode(), baseResponse.getMsg(), baseResponse.getErr());
                    }
                } else {
                    // 先杀死进程
                    WORK_THREAD.get(workInstance.getId()).interrupt();
                }
            }
        } finally {
            locker.clearLock("REQUEST_" + workInstance.getId());
        }
    }

    public Map<String, String> genSparkSubmitConfig(Map<String, String> sparkConfig) {

        // 过滤掉，前缀不包含spark.xxx的配置，spark submit中必须都是spark.xxx
        Map<String, String> sparkSubmitConfig = new HashMap<>();
        sparkConfig.forEach((k, v) -> {
            if (k.startsWith("spark")) {
                sparkSubmitConfig.put(k, v);
            }
        });
        return sparkSubmitConfig;
    }

    public Map<String, String> genSparkConfig(Map<String, String> sparkConfig) {

        // k8s的配置不能提交到作业中
        sparkConfig.remove("spark.kubernetes.driver.podTemplateFile");
        sparkConfig.remove("spark.kubernetes.executor.podTemplateFile");

        return sparkConfig;
    }
}
