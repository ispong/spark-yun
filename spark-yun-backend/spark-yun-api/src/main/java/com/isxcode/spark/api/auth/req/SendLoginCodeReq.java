package com.isxcode.spark.api.auth.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class SendLoginCodeReq {

    @Schema(title = "登录方式", example = "EMAIL")
    @NotEmpty(message = "登录方式不能为空")
    private String channel;

    @Schema(title = "接收账号", example = "user@example.com")
    @NotEmpty(message = "接收账号不能为空")
    private String receiver;
}
