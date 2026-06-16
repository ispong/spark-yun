package com.isxcode.spark.api.tenant.res;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantInviteRes {

    private String tenantId;

    private String inviteCode;

    private Integer validDays;

    private LocalDateTime expireDateTime;

    private List<String> roleIds;
}
