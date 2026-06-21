package com.isxcode.spark.api.agent.constants;

import com.isxcode.spark.api.main.constants.ModuleCode;

public interface SparkAgentUrl {

    String SUBMIT_WORK_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/submitWork";

    String SUBMIT_WORK_FOR_PY_SPARK = "/" + ModuleCode.SPARK_YUN_AGENT + "/submitWorkForPySpark";

    String GET_WORK_INFO_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/getWorkInfo";

    String GET_WORK_DATA_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/getWorkData";

    String GET_LAST_LINE_WORK_STDOUT_LOG_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/getLastLineWorkStdoutLog";

    String GET_CUSTOM_JAR_WORK_STDOUT_LOG_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/getCustomJarWorkStdoutLog";

    String GET_WORK_STDOUT_LOG_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/getWorkStdoutLog";

    String GET_WORK_STDERR_LOG_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/getWorkStderrLog";

    String STOP_WORK_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/stopWork";

    String HEART_CHECK_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/heartCheck";

    String CLEAN_AGENT_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/cleanAgent";

    String UPLOAD_AGENT_FILE_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/uploadAgentFile";

    String SUBMIT_LOCAL_SCRIPT_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/submitLocalScript";

    String GET_LOCAL_SCRIPT_STATUS_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/getLocalScriptStatus";

    String GET_LOCAL_SCRIPT_LOG_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/getLocalScriptLog";

    String CLEAN_LOCAL_SCRIPT_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/cleanLocalScript";

    String STOP_LOCAL_SCRIPT_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/stopLocalScript";

    String GET_NODE_MONITOR_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/getNodeMonitor";

    String CONTAINER_CHECK_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/containerCheck";

    String EXECUTE_CONTAINER_SQL_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/executeContainerSql";

    String DEPLOY_CONTAINER_URL = "/" + ModuleCode.SPARK_YUN_AGENT + "/deployContainer";
}
