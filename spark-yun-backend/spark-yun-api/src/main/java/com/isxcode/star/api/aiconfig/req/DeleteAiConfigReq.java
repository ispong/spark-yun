package com.isxcode.star.api.aiconfig.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class DeleteAiConfigReq {

    @Schema(title = "AI配置ID", example = "sy_xxx")
    @NotEmpty(message = "AI配置ID不能为空")
    private String id;
}
