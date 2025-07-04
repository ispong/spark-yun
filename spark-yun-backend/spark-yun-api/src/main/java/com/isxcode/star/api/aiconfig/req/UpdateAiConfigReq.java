package com.isxcode.star.api.aiconfig.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class UpdateAiConfigReq {

    @Schema(title = "AI配置ID", example = "sy_xxx")
    @NotEmpty(message = "AI配置ID不能为空")
    private String id;

    @Schema(title = "AI配置名称", example = "通义千问配置")
    @NotEmpty(message = "AI配置名称不能为空")
    private String name;

    @Schema(title = "AI模型类型", example = "QWEN")
    @NotEmpty(message = "AI模型类型不能为空")
    private String modelType;

    @Schema(title = "API地址", example = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation")
    @NotEmpty(message = "API地址不能为空")
    private String apiUrl;

    @Schema(title = "API密钥", example = "sk-xxx")
    @NotEmpty(message = "API密钥不能为空")
    private String apiKey;

    @Schema(title = "备注", example = "用于数据分析的AI配置")
    private String remark;
}
