package com.isxcode.spark.modules.ai.controller;

import com.isxcode.spark.api.ai.req.DeleteAiConfigReq;
import com.isxcode.spark.api.ai.req.PageAiConfigReq;
import com.isxcode.spark.api.ai.req.SaveAiConfigReq;
import com.isxcode.spark.api.ai.req.TestAiConfigReq;
import com.isxcode.spark.api.ai.res.AiConfigRes;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.common.annotations.successResponse.SuccessResponse;
import com.isxcode.spark.common.userlog.UserLog;
import com.isxcode.spark.modules.ai.service.AiConfigBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ai-config", description = "智能配置")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/ai-configs")
@Secured({RoleType.TENANT_SUPER_ADMIN, RoleType.TENANT_ADMIN})
public class AiConfigController {

    private final AiConfigBizService aiConfigBizService;

    @Operation(summary = "保存智能配置")
    @PostMapping("/save")
    @UserLog
    @SuccessResponse("保存成功")
    public void saveConfig(@Valid @RequestBody SaveAiConfigReq request) {

        aiConfigBizService.saveConfig(request);
    }

    @Operation(summary = "分页查询智能配置")
    @PostMapping("/page")
    @SuccessResponse("查询成功")
    public Page<AiConfigRes> pageConfig(@Valid @RequestBody PageAiConfigReq request) {

        return aiConfigBizService.pageConfig(request);
    }

    @Operation(summary = "查询可用智能配置")
    @PostMapping("/listEnabled")
    @SuccessResponse("查询成功")
    public List<AiConfigRes> listEnabledConfig() {

        return aiConfigBizService.listEnabledConfig();
    }

    @Operation(summary = "删除智能配置")
    @PostMapping("/delete")
    @UserLog
    @SuccessResponse("删除成功")
    public void deleteConfig(@Valid @RequestBody DeleteAiConfigReq request) {

        aiConfigBizService.deleteConfig(request);
    }

    @Operation(summary = "测试智能配置")
    @PostMapping("/test")
    @SuccessResponse("测试成功")
    public void testConfig(@Valid @RequestBody TestAiConfigReq request) {

        aiConfigBizService.testConfig(request);
    }
}
