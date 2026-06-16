package com.isxcode.spark.api.auth.req;

import com.isxcode.spark.backend.api.base.pojos.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PageLoginLogReq extends BasePageRequest {

    @Schema(title = "登录方式", example = "ACCOUNT_PASSWORD")
    private String loginMethod;

    @Schema(title = "登录结果", example = "SUCCESS")
    private String loginStatus;
}
