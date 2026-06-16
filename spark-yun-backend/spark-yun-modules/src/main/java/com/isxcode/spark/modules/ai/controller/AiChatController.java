package com.isxcode.spark.modules.ai.controller;

import com.isxcode.spark.api.ai.req.AiChatReq;
import com.isxcode.spark.api.ai.req.DeleteAiChatSessionReq;
import com.isxcode.spark.api.ai.req.DeleteAiPromptReq;
import com.isxcode.spark.api.ai.req.SaveAiChatSessionReq;
import com.isxcode.spark.api.ai.req.SaveAiPromptReq;
import com.isxcode.spark.api.ai.res.AiChatFileRes;
import com.isxcode.spark.api.ai.res.AiChatRes;
import com.isxcode.spark.api.ai.res.AiChatSessionRes;
import com.isxcode.spark.api.ai.res.AiConfigRes;
import com.isxcode.spark.api.ai.res.AiPromptRes;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.common.annotations.successResponse.SuccessResponse;
import com.isxcode.spark.modules.ai.service.AiConfigBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
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

    @Operation(summary = "查询AI聊天历史")
    @PostMapping("/chat/sessions")
    @SuccessResponse("查询成功")
    public List<AiChatSessionRes> listChatSessions() {

        return aiConfigBizService.listChatSessions();
    }

    @Operation(summary = "保存AI聊天历史")
    @PostMapping("/chat/session/save")
    @SuccessResponse("保存成功")
    public AiChatSessionRes saveChatSession(@Valid @RequestBody SaveAiChatSessionReq request) {

        return aiConfigBizService.saveChatSession(request);
    }

    @Operation(summary = "删除AI聊天历史")
    @PostMapping("/chat/session/delete")
    @SuccessResponse("删除成功")
    public void deleteChatSession(@Valid @RequestBody DeleteAiChatSessionReq request) {

        aiConfigBizService.deleteChatSession(request);
    }

    @Operation(summary = "查询AI提示词")
    @PostMapping("/prompts")
    @SuccessResponse("查询成功")
    public List<AiPromptRes> listPrompts() {

        return aiConfigBizService.listPrompts();
    }

    @Operation(summary = "保存AI提示词")
    @PostMapping("/prompt/save")
    @SuccessResponse("保存成功")
    public AiPromptRes savePrompt(@Valid @RequestBody SaveAiPromptReq request) {

        return aiConfigBizService.savePrompt(request);
    }

    @Operation(summary = "删除AI提示词")
    @PostMapping("/prompt/delete")
    @SuccessResponse("删除成功")
    public void deletePrompt(@Valid @RequestBody DeleteAiPromptReq request) {

        aiConfigBizService.deletePrompt(request);
    }

    @Operation(summary = "解析AI对话附件")
    @PostMapping(value = "/chat/file/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SuccessResponse("解析成功")
    public AiChatFileRes parseChatFile(@RequestParam("file") MultipartFile file) {

        return aiConfigBizService.parseChatFile(file);
    }

    @Operation(summary = "AI流式对话")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> streamChat(@Valid @RequestBody AiChatReq request) {

        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM)
            .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform").header(HttpHeaders.CONNECTION, "keep-alive")
            .header("X-Accel-Buffering", "no").body(aiConfigBizService.streamChat(request));
    }
}
