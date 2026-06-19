package com.isxcode.spark.api.userlog.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserLogDefinitionRes {

    private String moduleCode;

    private String moduleName;

    private String logType;

    private String apiName;
}
