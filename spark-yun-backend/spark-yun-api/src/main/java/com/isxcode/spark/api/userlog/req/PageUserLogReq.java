package com.isxcode.spark.api.userlog.req;

import com.isxcode.spark.backend.api.base.pojos.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PageUserLogReq extends BasePageRequest {

    @Schema(title = "模块编码", example = "USER")
    private String moduleCode;

    @Schema(title = "日志归属类型", example = "PLATFORM")
    private String logType;

    @Schema(title = "账号")
    private String account;

    @Schema(title = "租户id")
    private String tenantId;

    @Schema(title = "开始时间")
    private LocalDateTime startDateTime;

    @Schema(title = "结束时间")
    private LocalDateTime endDateTime;
}
