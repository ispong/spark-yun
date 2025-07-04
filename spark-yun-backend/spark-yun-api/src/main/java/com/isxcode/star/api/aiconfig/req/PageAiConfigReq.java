package com.isxcode.star.api.aiconfig.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class PageAiConfigReq {

    @Schema(title = "页码", example = "0")
    @NotNull(message = "页码不能为空")
    private Integer page;

    @Schema(title = "页大小", example = "10")
    @NotNull(message = "页大小不能为空")
    private Integer size;

    @Schema(title = "搜索关键字", example = "通义千问")
    private String searchKeyWord;
}
