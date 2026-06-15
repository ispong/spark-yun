package com.isxcode.spark.api.ai.req;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageAiConfigReq {

    @Min(0)
    private Integer page = 0;

    @Min(1)
    private Integer pageSize = 10;

    private String searchKeyWord = "";
}
