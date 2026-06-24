package com.isxcode.spark.api.user.req;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateMyLocaleReq {

    @NotEmpty(message = "locale不能为空")
    @Pattern(regexp = "zh-CN|en-US", message = "locale只支持zh-CN和en-US")
    private String locale;
}
