package com.isxcode.spark.agent.controller;

import com.isxcode.spark.agent.service.SparkAgentBizService;
import com.isxcode.spark.api.agent.constants.SparkAgentUrl;
import com.isxcode.spark.api.agent.req.spark.*;
import com.isxcode.spark.api.agent.res.spark.*;
import com.isxcode.spark.api.monitor.dto.NodeMonitorInfo;
import com.isxcode.spark.api.work.res.AgentLinkResponse;
import com.isxcode.spark.common.annotations.successResponse.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Tag(name = "spark-agent", description = "Spark相关代理模块")
@RestController
@RequiredArgsConstructor
public class SparkAgentController {

    private final SparkAgentBizService sparkYunAgentBizService;

    @Operation(summary = "提交作业")
    @PostMapping(SparkAgentUrl.SUBMIT_WORK_URL)
    @SuccessResponse("提交成功")
    public SubmitWorkRes submitWork(@Valid @RequestBody SubmitWorkReq submitWorkReq) {

        return sparkYunAgentBizService.submitWork(submitWorkReq);
    }

    @Operation(summary = "提交作业接口")
    @PostMapping(SparkAgentUrl.SUBMIT_WORK_FOR_PY_SPARK)
    @SuccessResponse("提交成功")
    public SubmitWorkRes submitWorkForPySpark(@Valid @RequestBody SubmitWorkReq submitWorkReq) {

        return sparkYunAgentBizService.submitWorkForPySpark(submitWorkReq);
    }

    @Operation(summary = "获取作业状态")
    @PostMapping(SparkAgentUrl.GET_WORK_INFO_URL)
    @SuccessResponse("获取成功")
    public GetWorkInfoRes getWorkStatus(@Valid @RequestBody GetWorkStatusReq getWorkStatusReq) {

        return sparkYunAgentBizService.getWorkInfo(getWorkStatusReq);
    }

    @Operation(summary = "获取返回数据")
    @PostMapping(SparkAgentUrl.GET_WORK_DATA_URL)
    @SuccessResponse("获取成功")
    public GetWorkDataRes getWorkData(@Valid @RequestBody GetWorkDataReq getWorkDataReq) {

        return sparkYunAgentBizService.getWorkData(getWorkDataReq);
    }

    @Operation(summary = "获取自定义Jar作业Stdout日志")
    @PostMapping(SparkAgentUrl.GET_CUSTOM_JAR_WORK_STDOUT_LOG_URL)
    @SuccessResponse("获取成功")
    public GetWorkStdoutLogRes getCustomJarWorkStdoutLog(@Valid @RequestBody GetWorkStdoutLogReq getWorkStdoutLogReq) {

        return sparkYunAgentBizService.getCustomJarWorkStdoutLog(getWorkStdoutLogReq);
    }

    @Operation(summary = "获取Stdout日志")
    @PostMapping(SparkAgentUrl.GET_WORK_STDOUT_LOG_URL)
    @SuccessResponse("获取成功")
    public GetWorkStdoutLogRes getWorkStdoutLog(@Valid @RequestBody GetWorkStdoutLogReq getWorkStdoutLogReq) {

        return sparkYunAgentBizService.getWorkStdoutLog(getWorkStdoutLogReq);
    }

    @Operation(summary = "获取Stderr日志")
    @PostMapping(SparkAgentUrl.GET_WORK_STDERR_LOG_URL)
    @SuccessResponse("获取成功")
    public GetWorkStderrLogRes getWorkStderrLog(@Valid @RequestBody GetWorkStderrLogReq getWorkStderrLogReq) {

        return sparkYunAgentBizService.getWorkStderrLog(getWorkStderrLogReq);
    }

    @Operation(summary = "获取最后一行Stdout日志")
    @PostMapping(SparkAgentUrl.GET_LAST_LINE_WORK_STDOUT_LOG_URL)
    @SuccessResponse("获取成功")
    public GetWorkStdoutLogRes getLastLineWorkStdoutLog(@Valid @RequestBody GetWorkStdoutLogReq getWorkStdoutLogReq) {

        return sparkYunAgentBizService.getLastLineWorkStdoutLog(getWorkStdoutLogReq);
    }

    @Operation(summary = "中止作业")
    @PostMapping(SparkAgentUrl.STOP_WORK_URL)
    @SuccessResponse("中止成功")
    public AgentLinkResponse stopWork(@Valid @RequestBody StopWorkReq stopWorkReq) {

        sparkYunAgentBizService.stopWork(stopWorkReq);
        return AgentLinkResponse.builder().msg("中止成功").build();
    }

    @Operation(summary = "代理服务心跳检测")
    @PostMapping(SparkAgentUrl.HEART_CHECK_URL)
    @SuccessResponse("正常心跳")
    public AgentLinkResponse heartCheck() {

        return AgentLinkResponse.builder().msg("心跳正常").build();
    }

    @Operation(summary = "清理代理缓存")
    @PostMapping(SparkAgentUrl.CLEAN_AGENT_URL)
    @SuccessResponse("清理成功")
    public AgentLinkResponse cleanAgent(@RequestBody CleanAgentReq cleanAgentReq) {

        return sparkYunAgentBizService.cleanAgent(cleanAgentReq);
    }

    @Operation(summary = "上传代理文件")
    @PostMapping(SparkAgentUrl.UPLOAD_AGENT_FILE_URL)
    @SuccessResponse("上传成功")
    public AgentLinkResponse uploadAgentFile(@RequestBody UploadAgentFileReq uploadAgentFileReq) {

        return sparkYunAgentBizService.uploadAgentFile(uploadAgentFileReq);
    }

    @Operation(summary = "提交本地脚本")
    @PostMapping(SparkAgentUrl.SUBMIT_LOCAL_SCRIPT_URL)
    @SuccessResponse("提交成功")
    public AgentLinkResponse submitLocalScript(@RequestBody SubmitLocalScriptReq submitLocalScriptReq) {

        return sparkYunAgentBizService.submitLocalScript(submitLocalScriptReq);
    }

    @Operation(summary = "获取本地脚本状态")
    @PostMapping(SparkAgentUrl.GET_LOCAL_SCRIPT_STATUS_URL)
    @SuccessResponse("获取成功")
    public AgentLinkResponse getLocalScriptStatus(@RequestBody LocalScriptStatusReq localScriptStatusReq) {

        return sparkYunAgentBizService.getLocalScriptStatus(localScriptStatusReq);
    }

    @Operation(summary = "获取本地脚本日志")
    @PostMapping(SparkAgentUrl.GET_LOCAL_SCRIPT_LOG_URL)
    @SuccessResponse("获取成功")
    public AgentLinkResponse getLocalScriptLog(@RequestBody LocalScriptLogReq localScriptLogReq) {

        return sparkYunAgentBizService.getLocalScriptLog(localScriptLogReq);
    }

    @Operation(summary = "清理本地脚本")
    @PostMapping(SparkAgentUrl.CLEAN_LOCAL_SCRIPT_URL)
    @SuccessResponse("清理成功")
    public AgentLinkResponse cleanLocalScript(@RequestBody CleanLocalScriptReq cleanLocalScriptReq) {

        return sparkYunAgentBizService.cleanLocalScript(cleanLocalScriptReq);
    }

    @Operation(summary = "中止本地脚本")
    @PostMapping(SparkAgentUrl.STOP_LOCAL_SCRIPT_URL)
    @SuccessResponse("中止成功")
    public AgentLinkResponse stopLocalScript(@RequestBody StopLocalScriptReq stopLocalScriptReq) {

        return sparkYunAgentBizService.stopLocalScript(stopLocalScriptReq);
    }

    @Operation(summary = "获取节点监控")
    @PostMapping(SparkAgentUrl.GET_NODE_MONITOR_URL)
    @SuccessResponse("获取成功")
    public NodeMonitorInfo getNodeMonitor() {

        return sparkYunAgentBizService.getNodeMonitor();
    }

    @Operation(summary = "计算容器心跳检测")
    @PostMapping(SparkAgentUrl.CONTAINER_CHECK_URL)
    @SuccessResponse("检测成功")
    public ContainerCheckRes containerCheck(@RequestBody ContainerCheckReq containerCheckReq) {

        return sparkYunAgentBizService.containerCheck(containerCheckReq);
    }

    @Operation(summary = "执行计算容器SQL")
    @PostMapping(SparkAgentUrl.EXECUTE_CONTAINER_SQL_URL)
    @SuccessResponse("提交成功")
    public ExecuteContainerSqlRes executeContainerSql(@RequestBody ExecuteContainerSqlReq executeContainerSqlReq) {

        return sparkYunAgentBizService.executeContainerSql(executeContainerSqlReq);
    }

    @Operation(summary = "部署计算容器")
    @PostMapping(SparkAgentUrl.DEPLOY_CONTAINER_URL)
    @SuccessResponse("提交成功")
    public DeployContainerRes deployContainer(@Valid @RequestBody SubmitWorkReq submitWorkReq) {

        return sparkYunAgentBizService.deployContainer(submitWorkReq);
    }
}
