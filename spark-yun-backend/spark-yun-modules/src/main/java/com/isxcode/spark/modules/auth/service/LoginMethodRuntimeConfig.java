package com.isxcode.spark.modules.auth.service;

import com.isxcode.spark.api.auth.dto.LoginMethodConfigDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginMethodRuntimeConfig {

    private String defaultLoginMethod;

    private Boolean accountEnabled;

    private Boolean accountPasswordEnabled;

    private Boolean accountPhonePasswordEnabled;

    private Boolean accountEmailPasswordEnabled;

    private Boolean emailEnabled;

    private Boolean emailRegisterEnabled;

    private Boolean phoneEnabled;

    private Boolean phoneRegisterEnabled;

    private LoginMethodConfigDto config;
}
