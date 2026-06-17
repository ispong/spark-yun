package com.isxcode.spark.api.authorization.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PageRoleInstanceResourceRes {

    private String id;

    private String name;

    private String type;

    private String status;

    private String remark;
}
