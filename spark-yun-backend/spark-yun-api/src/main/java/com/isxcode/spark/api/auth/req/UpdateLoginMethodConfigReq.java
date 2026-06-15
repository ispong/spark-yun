package com.isxcode.spark.api.auth.req;

import com.isxcode.spark.api.auth.dto.LoginMethodConfigDto;
import lombok.Data;

@Data
public class UpdateLoginMethodConfigReq {

    private String defaultLoginMethod;

    private Boolean accountEnabled;

    private Boolean accountPhonePasswordEnabled;

    private Boolean accountEmailPasswordEnabled;

    private Boolean emailEnabled;

    private Boolean emailRegisterEnabled;

    private Boolean phoneEnabled;

    private Boolean phoneRegisterEnabled;

    private Boolean autoCreateTenant;

    private LoginMethodConfigDto config;
}
