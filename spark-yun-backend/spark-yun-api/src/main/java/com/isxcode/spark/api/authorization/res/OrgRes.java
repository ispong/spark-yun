package com.isxcode.spark.api.authorization.res;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrgRes {

    private String id;

    private String parentId;

    private String name;

    private List<String> userIds;

    private List<String> roleIds;
}
