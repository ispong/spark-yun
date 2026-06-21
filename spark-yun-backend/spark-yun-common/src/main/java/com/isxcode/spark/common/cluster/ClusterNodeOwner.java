package com.isxcode.spark.common.cluster;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class ClusterNodeOwner {

    private static final String HOST_NAME = resolveHostName();

    private static final String OWNER = resolveOwner();

    private ClusterNodeOwner() {}

    public static String getOwner() {

        return OWNER;
    }

    public static String getHostName() {

        return HOST_NAME;
    }

    private static String resolveOwner() {

        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isBlank()) {
            return hostname;
        }

        if (HOST_NAME != null && !HOST_NAME.isBlank()) {
            return HOST_NAME;
        }

        return "local-" + ManagementFactory.getRuntimeMXBean().getName();
    }

    private static String resolveHostName() {

        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
