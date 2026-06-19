package com.isxcode.spark.api.user.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class SendUpdateEmailCodeReq {

    @Schema(title = "邮箱", example = "user@example.com")
    @NotEmpty(message = "邮箱不能为空")
    private String email;
}
