package com.isxcode.spark.common.userlog;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserLogDefinition {

    private String moduleCode;

    private String moduleName;

    private String logType;

    private String actionCode;

    private String actionName;

    private String apiName;
}
