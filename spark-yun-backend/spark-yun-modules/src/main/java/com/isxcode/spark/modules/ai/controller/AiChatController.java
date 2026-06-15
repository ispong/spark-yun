package com.isxcode.spark.modules.ai.controller;

import com.isxcode.spark.api.ai.req.AiChatReq;
import com.isxcode.spark.api.ai.res.AiChatRes;
import com.isxcode.spark.api.ai.res.AiConfigRes;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.common.annotations.successResponse.SuccessResponse;
import com.isxcode.spark.modules.ai.service.AiConfigBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Tag(name = "ai-chat", description = "至轻智能")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspace/ai")
@Secured({RoleType.TENANT_MEMBER, RoleType.TENANT_SUPER_ADMIN, RoleType.TENANT_ADMIN})
public class AiChatController {

    private final AiConfigBizService aiConfigBizService;

    @Operation(summary = "查询可用智能配置")
    @PostMapping("/configs")
    @SuccessResponse("查询成功")
    public List<AiConfigRes> listEnabledConfig() {

        return aiConfigBizService.listEnabledConfig();
    }

    @Operation(summary = "AI对话")
    @PostMapping("/chat")
    @SuccessResponse("对话成功")
    public AiChatRes chat(@Valid @RequestBody AiChatReq request) {

        return aiConfigBizService.chat(request);
    }

    @Operation(summary = "AI流式对话")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> streamChat(@Valid @RequestBody AiChatReq request) {

        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(aiConfigBizService.streamChat(request));
    }
}
