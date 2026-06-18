package com.isxcode.spark.api.cluster.constants;

/** Cluster node connection type. */
public interface ClusterNodeConnectType {

    /** Connect through SSH and let the platform manage the agent lifecycle. */
    String SSH = "SSH";

    /** Connect to an agent that has already been started on the node. */
    String AGENT_PORT = "AGENT_PORT";
}
