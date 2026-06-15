package com.isxcode.spark.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PhoneLoginConfig {

    @Schema(title = "短信服务商", example = "ALIYUN")
    private String provider;

    @Schema(title = "阿里云地域", example = "cn-hangzhou")
    private String regionId;

    @Schema(title = "阿里云AccessKeyId")
    private String accessKeyId;

    @Schema(title = "阿里云AccessKeySecret")
    private String accessKeySecret;

    @Schema(title = "短信签名")
    private String signName;

    @Schema(title = "短信模板")
    private String templateCode;

    @Schema(title = "验证码模板变量名", example = "code")
    private String templateParamName;
}
