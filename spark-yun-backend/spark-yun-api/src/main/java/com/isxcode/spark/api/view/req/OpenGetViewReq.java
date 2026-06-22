package com.isxcode.spark.api.view.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class OpenGetViewReq {

    @Schema(title = "分享链接id", example = "sy_123")
    @NotEmpty(message = "viewLinkId不能为空")
    private String viewLinkId;

    @Schema(title = "大屏id", example = "sy_123")
    @NotEmpty(message = "id不能为空")
    private String id;
}
