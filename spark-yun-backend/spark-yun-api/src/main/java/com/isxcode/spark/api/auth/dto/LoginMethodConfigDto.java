package com.isxcode.spark.api.auth.dto;

import lombok.Data;

@Data
public class LoginMethodConfigDto {

    private EmailLoginConfig emailConfig;

    private PhoneLoginConfig phoneConfig;
}
