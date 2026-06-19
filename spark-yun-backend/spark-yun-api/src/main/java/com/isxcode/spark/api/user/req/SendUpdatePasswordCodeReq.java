package com.isxcode.spark.api.user.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class SendUpdatePasswordCodeReq {

    @Schema(title = "验证方式", example = "PHONE")
    @NotEmpty(message = "验证方式不能为空")
    private String channel;
}
