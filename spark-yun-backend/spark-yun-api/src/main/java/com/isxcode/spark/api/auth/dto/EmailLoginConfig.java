package com.isxcode.spark.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class EmailLoginConfig {

    @Schema(title = "邮箱服务商", example = "QQ")
    private String provider;

    @Schema(title = "SMTP服务器", example = "smtp.example.com")
    private String host;

    @Schema(title = "SMTP端口", example = "587")
    private Integer port;

    @Schema(title = "SMTP用户名")
    private String username;

    @Schema(title = "SMTP密码")
    private String password;

    @Schema(title = "发件邮箱")
    private String fromAddress;

    @Schema(title = "发件人名称")
    private String fromName;

    @Schema(title = "是否启用SSL")
    private Boolean ssl;

    @Schema(title = "是否启用STARTTLS")
    private Boolean startTls;

    @Schema(title = "邮件标题", example = "至轻云登录验证码")
    private String subject;
}
