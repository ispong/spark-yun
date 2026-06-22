package com.isxcode.spark.api.ai.ao;

import java.io.Serializable;

public record AiMcpToken(String userId, String tenantId, String configId, String scope) implements Serializable {
}
