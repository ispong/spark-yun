package com.isxcode.spark.modules.ai.controller;

import com.isxcode.spark.backend.api.base.constants.SecurityConstants;
import com.isxcode.spark.modules.ai.service.AiConfigBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ai-mcp", description = "至轻智能MCP")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspace/ai/mcp")
public class AiMcpController {

    private final AiConfigBizService aiConfigBizService;

    @Operation(summary = "AI MCP Streamable HTTP")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> handleMcp(
        @RequestHeader(value = SecurityConstants.HEADER_AUTHORIZATION, required = false) String authorization,
        @RequestBody Object payload) {

        Object response = aiConfigBizService.handleMcpPayload(authorization, payload);
        if (response == null) {
            return ResponseEntity.accepted().header(HttpHeaders.CACHE_CONTROL, "no-cache").build();
        }
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-cache").body(response);
    }
}
