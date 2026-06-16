package com.isxcode.spark.api.tenant.req;

import java.util.List;
import lombok.Data;

@Data
public class SaveTenantInviteReq {

    private String tenantId;

    private Integer validDays;

    private List<String> roleIds;

    private Boolean regenerate;
}
