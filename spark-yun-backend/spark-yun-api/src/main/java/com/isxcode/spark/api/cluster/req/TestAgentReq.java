package com.isxcode.spark.api.cluster.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
public class TestAgentReq {

    @Schema(title = "节点服务器hostname", example = "192.168.115.103")
    @NotEmpty(message = "host不能为空")
    private String host;

    @Schema(title = "节点服务器ssh端口号", example = "22")
    private String port;

    @Schema(title = "节点服务器用户名", example = "ispong")
    private String username;

    @Schema(title = "节点服务器密码", example = "ispong123")
    private String passwd;

    @Schema(title = "代理服务端口号", example = "30177")
    private String agentPort;

    @Schema(title = "连接方式", example = "SSH")
    private String connectType;
}
