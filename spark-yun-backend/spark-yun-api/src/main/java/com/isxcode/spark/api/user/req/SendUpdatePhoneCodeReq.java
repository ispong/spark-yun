package com.isxcode.spark.api.user.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class SendUpdatePhoneCodeReq {

    @Schema(title = "手机号", example = "13800138000")
    @NotEmpty(message = "手机号不能为空")
    private String phone;
}
