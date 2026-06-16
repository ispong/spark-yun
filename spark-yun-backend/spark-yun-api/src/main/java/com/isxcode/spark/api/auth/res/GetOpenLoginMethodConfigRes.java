package com.isxcode.spark.api.auth.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetOpenLoginMethodConfigRes {

    private String defaultLoginMethod;

    private Boolean accountEnabled;

    private Boolean accountPasswordEnabled;

    private Boolean accountPhonePasswordEnabled;

    private Boolean accountEmailPasswordEnabled;

    private Boolean emailEnabled;

    private Boolean emailRegisterEnabled;

    private Boolean phoneEnabled;

    private Boolean phoneRegisterEnabled;
}
