package com.isxcode.star.api.aiconfig.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageAiConfigRes {

    @Schema(title = "AI配置ID", example = "sy_xxx")
    private String id;

    @Schema(title = "AI配置名称", example = "通义千问配置")
    private String name;

    @Schema(title = "AI模型类型", example = "QWEN")
    private String modelType;

    @Schema(title = "API地址", example = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation")
    private String apiUrl;

    @Schema(title = "备注", example = "用于数据分析的AI配置")
    private String remark;

    @Schema(title = "状态", example = "ENABLE")
    private String status;

    @Schema(title = "创建时间")
    private LocalDateTime createDateTime;

    @Schema(title = "更新时间")
    private LocalDateTime lastModifiedDateTime;
}
