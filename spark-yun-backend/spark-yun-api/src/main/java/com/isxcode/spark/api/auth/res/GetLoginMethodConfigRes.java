package com.isxcode.spark.api.auth.res;

import com.isxcode.spark.api.auth.dto.LoginMethodConfigDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetLoginMethodConfigRes {

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
