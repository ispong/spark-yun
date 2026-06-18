package com.isxcode.spark.api.agent.constants;

/**
 * 代理ks8s相关的静态配置.
 */
public interface AgentKubernetes {

    String SPARK_DOCKER_IMAGE = "apache/spark:3.5.8-java17-python3";

    String FLINK_DOCKER_IMAGE = "apache/flink:1.20.4-java17";

    String NAMESPACE = "zhiqingyun-space";

    String SERVICE_ACCOUNT_NAME = "zhiqingyun";

    String PULL_POLICY = "IfNotPresent";
}
