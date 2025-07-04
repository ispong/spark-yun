package com.isxcode.star.modules.aiconfig.controller;

import com.isxcode.star.api.aiconfig.req.AddAiConfigReq;
import com.isxcode.star.api.aiconfig.req.DeleteAiConfigReq;
import com.isxcode.star.api.aiconfig.req.PageAiConfigReq;
import com.isxcode.star.api.aiconfig.req.UpdateAiConfigReq;
import com.isxcode.star.api.aiconfig.res.PageAiConfigRes;
import com.isxcode.star.api.main.constants.ModuleCode;
import com.isxcode.star.api.user.constants.RoleType;
import com.isxcode.star.common.annotations.successResponse.SuccessResponse;
import com.isxcode.star.modules.aiconfig.service.biz.AiConfigBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Tag(name = "AI配置模块")
@RequestMapping(ModuleCode.AI_CONFIG)
@RestController
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigBizService aiConfigBizService;

    @Secured({RoleType.TENANT_ADMIN})
    @Operation(summary = "添加AI配置接口")
    @PostMapping("/addAiConfig")
    @SuccessResponse("添加成功")
    public void addAiConfig(@Valid @RequestBody AddAiConfigReq addAiConfigReq) {

        aiConfigBizService.addAiConfig(addAiConfigReq);
    }

    @Secured({RoleType.TENANT_ADMIN, RoleType.TENANT_MEMBER})
    @Operation(summary = "分页查询AI配置接口")
    @PostMapping("/pageAiConfig")
    @SuccessResponse("查询成功")
    public Page<PageAiConfigRes> pageAiConfig(@Valid @RequestBody PageAiConfigReq pageAiConfigReq) {

        return aiConfigBizService.pageAiConfig(pageAiConfigReq);
    }

    @Secured({RoleType.TENANT_ADMIN})
    @Operation(summary = "更新AI配置接口")
    @PostMapping("/updateAiConfig")
    @SuccessResponse("更新成功")
    public void updateAiConfig(@Valid @RequestBody UpdateAiConfigReq updateAiConfigReq) {

        aiConfigBizService.updateAiConfig(updateAiConfigReq);
    }

    @Secured({RoleType.TENANT_ADMIN})
    @Operation(summary = "删除AI配置接口")
    @PostMapping("/deleteAiConfig")
    @SuccessResponse("删除成功")
    public void deleteAiConfig(@Valid @RequestBody DeleteAiConfigReq deleteAiConfigReq) {

        aiConfigBizService.deleteAiConfig(deleteAiConfigReq);
    }

    @Secured({RoleType.TENANT_ADMIN})
    @Operation(summary = "启用AI配置接口")
    @PostMapping("/enableAiConfig")
    @SuccessResponse("启用成功")
    public void enableAiConfig(@Valid @RequestBody DeleteAiConfigReq deleteAiConfigReq) {

        aiConfigBizService.enableAiConfig(deleteAiConfigReq.getId());
    }

    @Secured({RoleType.TENANT_ADMIN})
    @Operation(summary = "禁用AI配置接口")
    @PostMapping("/disableAiConfig")
    @SuccessResponse("禁用成功")
    public void disableAiConfig(@Valid @RequestBody DeleteAiConfigReq deleteAiConfigReq) {

        aiConfigBizService.disableAiConfig(deleteAiConfigReq.getId());
    }

    @Secured({RoleType.TENANT_ADMIN})
    @Operation(summary = "测试AI配置接口")
    @PostMapping("/testAiConfig")
    @SuccessResponse("测试成功")
    public String testAiConfig(@Valid @RequestBody AddAiConfigReq addAiConfigReq) {

        return aiConfigBizService.testAiConfig(addAiConfigReq);
    }
}
