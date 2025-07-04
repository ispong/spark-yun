package com.isxcode.star.modules.work.run.impl;

import com.alibaba.fastjson2.JSON;
import com.isxcode.star.api.aiconfig.constants.AiModelType;
import com.isxcode.star.api.instance.constants.InstanceStatus;
import com.isxcode.star.api.work.constants.WorkType;
import com.isxcode.star.backend.api.base.exceptions.WorkRunException;
import com.isxcode.star.backend.api.base.exceptions.IsxAppException;
import com.isxcode.star.common.utils.aes.AesUtils;
import com.isxcode.star.modules.aiconfig.entity.AiConfigEntity;
import com.isxcode.star.modules.aiconfig.repository.AiConfigRepository;
import com.isxcode.star.modules.alarm.service.AlarmService;
import com.isxcode.star.modules.work.entity.WorkInstanceEntity;
import com.isxcode.star.modules.work.repository.WorkInstanceRepository;
import com.isxcode.star.modules.work.run.WorkExecutor;
import com.isxcode.star.modules.work.run.WorkRunContext;
import com.isxcode.star.api.work.constants.WorkLog;
import com.isxcode.star.modules.workflow.repository.WorkflowInstanceRepository;
import com.isxcode.star.modules.work.sql.SqlFunctionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class AiChatExecutor extends WorkExecutor {

    private final AiConfigRepository aiConfigRepository;
    private final AesUtils aesUtils;
    private final RestTemplate restTemplate;

    public AiChatExecutor(WorkInstanceRepository workInstanceRepository,
        WorkflowInstanceRepository workflowInstanceRepository, AlarmService alarmService,
        SqlFunctionService sqlFunctionService, AiConfigRepository aiConfigRepository, AesUtils aesUtils) {
        super(workInstanceRepository, workflowInstanceRepository, alarmService, sqlFunctionService);
        this.aiConfigRepository = aiConfigRepository;
        this.aesUtils = aesUtils;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String getWorkType() {
        return WorkType.AI_CHAT;
    }

    @Override
    protected void execute(WorkRunContext workRunContext, WorkInstanceEntity workInstance) {

        // 将线程存到Map
        WORK_THREAD.put(workInstance.getId(), Thread.currentThread());

        // 获取日志构造器
        StringBuilder logBuilder = workRunContext.getLogBuilder();

        try {
            // 检查AI配置
            logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("开始检测AI配置 \n");

            String aiConfigId = workRunContext.getAiConfigId();
            if (aiConfigId == null || aiConfigId.isEmpty()) {
                throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "检测AI配置失败: 未配置AI模型  \n");
            }

            Optional<AiConfigEntity> aiConfigOptional = aiConfigRepository.findById(aiConfigId);
            if (!aiConfigOptional.isPresent()) {
                throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "检测AI配置失败: AI配置不存在  \n");
            }

            AiConfigEntity aiConfig = aiConfigOptional.get();
            logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("AI配置检测成功，模型类型: ")
                .append(aiConfig.getModelType()).append(" \n");

            // 检查脚本内容
            String script = workRunContext.getScript();
            if (script == null || script.trim().isEmpty()) {
                throw new WorkRunException(LocalDateTime.now() + WorkLog.ERROR_INFO + "检测脚本失败: AI对话内容为空  \n");
            }

            logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("开始调用AI接口 \n");

            // 调用AI接口
            String response = callAiApi(aiConfig, script, logBuilder);

            // 保存原始JSON响应到结果数据（用于运行结果tab的JSON格式化显示和结果展示tab的content提取）
            if (response != null && !response.trim().isEmpty()) {
                workInstance.setResultData(response);

                // 在运行日志中添加简要的AI回复信息
                logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("AI回复成功，请查看运行结果和结果展示标签页 \n");
            } else {
                workInstance.setResultData("{\"error\": \"AI未返回有效回复\"}");
                logBuilder.append(LocalDateTime.now()).append(WorkLog.ERROR_INFO).append("AI未返回有效回复 \n");
            }
            workInstance.setStatus(InstanceStatus.SUCCESS);
            logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("AI对话完成 \n");

        } catch (WorkRunException e) {
            workInstance.setStatus(InstanceStatus.FAIL);
            logBuilder.append(e.getMessage());
        } catch (Exception e) {
            workInstance.setStatus(InstanceStatus.FAIL);
            logBuilder.append(LocalDateTime.now()).append(WorkLog.ERROR_INFO).append("AI对话执行异常: ")
                .append(e.getMessage()).append(" \n");
        } finally {
            // 更新实例状态
            workInstance.setSubmitLog(logBuilder.toString());
            updateInstance(workInstance, logBuilder);
            WORK_THREAD.remove(workInstance.getId());
        }
    }

    private String callAiApi(AiConfigEntity aiConfig, String prompt, StringBuilder logBuilder) {
        try {
            // 解密API密钥，如果解密失败则使用原始值（兼容明文存储）
            String apiKey;
            try {
                apiKey = aesUtils.decrypt(aiConfig.getApiKey());
            } catch (Exception e) {
                // 解密失败，可能是明文存储，直接使用原始值
                apiKey = aiConfig.getApiKey();
                logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO)
                    .append("使用明文API密钥（建议重新保存配置以启用加密） \n");
            }

            // 根据不同的AI模型类型构建请求
            Map<String, Object> requestBody = buildRequestBody(aiConfig, prompt);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (AiModelType.QWEN.equals(aiConfig.getModelType())) {
                headers.set("Authorization", "Bearer " + apiKey);
            } else if (AiModelType.CHATGPT_4O.equals(aiConfig.getModelType())) {
                headers.set("Authorization", "Bearer " + apiKey);
            } else if (AiModelType.GEMINI.equals(aiConfig.getModelType())) {
                headers.set("x-goog-api-key", apiKey);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<String> response =
                restTemplate.exchange(aiConfig.getApiUrl(), HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("AI接口调用成功 \n");

                // 解析响应
                return parseResponse(aiConfig.getModelType(), responseBody);
            } else {
                throw new IsxAppException("AI接口调用失败，状态码: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logBuilder.append(LocalDateTime.now()).append(WorkLog.ERROR_INFO).append("AI接口调用异常: ")
                .append(e.getMessage()).append(" \n");
            throw new IsxAppException("AI接口调用失败: " + e.getMessage());
        }
    }

    private Map<String, Object> buildRequestBody(AiConfigEntity aiConfig, String prompt) {
        Map<String, Object> requestBody = new HashMap<>();

        if (AiModelType.QWEN.equals(aiConfig.getModelType())) {
            // 通义千问请求格式
            Map<String, Object> input = new HashMap<>();
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            input.put("messages", messages);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("result_format", "message");

            requestBody.put("model", "qwen-turbo");
            requestBody.put("input", input);
            requestBody.put("parameters", parameters);

        } else if (AiModelType.CHATGPT_4O.equals(aiConfig.getModelType())) {
            // ChatGPT请求格式
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);

            requestBody.put("model", "gpt-4o");
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 1000);

        } else if (AiModelType.GEMINI.equals(aiConfig.getModelType())) {
            // Gemini请求格式
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, String>> parts = new ArrayList<>();
            Map<String, String> part = new HashMap<>();
            part.put("text", prompt);
            parts.add(part);
            content.put("parts", parts);
            contents.add(content);

            requestBody.put("contents", contents);
        }

        return requestBody;
    }

    private String parseResponse(String modelType, String responseBody) {
        try {
            log.info("开始解析AI响应，模型类型: {}, 响应内容: {}", modelType, responseBody);
            Map<String, Object> response = JSON.parseObject(responseBody, Map.class);

            if (AiModelType.QWEN.equals(modelType)) {
                // 解析通义千问响应
                Map<String, Object> output = (Map<String, Object>) response.get("output");
                if (output != null) {
                    Map<String, Object> message = (Map<String, Object>) output.get("message");
                    if (message != null) {
                        String content = (String) message.get("content");
                        log.info("通义千问解析结果: {}", content);
                        return content;
                    }
                }
            } else if (AiModelType.CHATGPT_4O.equals(modelType)) {
                // 解析ChatGPT响应
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            } else if (AiModelType.GEMINI.equals(modelType)) {
                // 解析Gemini响应
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                    if (content != null) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            return (String) parts.get(0).get("text");
                        }
                    }
                }
            }

            // 如果没有匹配的模型类型或解析失败，尝试通用解析
            log.warn("未找到匹配的模型类型或解析失败，模型类型: {}, 尝试通用解析", modelType);

            // 尝试通用解析逻辑
            String content = tryGenericParse(response);
            if (content != null && !content.trim().isEmpty()) {
                log.info("通用解析成功: {}", content);
                return content;
            }

            log.warn("通用解析也失败，返回原始响应");
            return responseBody; // 如果解析失败，返回原始响应

        } catch (Exception e) {
            log.error("解析AI响应失败", e);
            return responseBody; // 解析失败时返回原始响应
        }
    }

    /**
     * 通用AI响应解析方法
     */
    private String tryGenericParse(Map<String, Object> response) {
        try {
            // 尝试常见的响应结构

            // 1. 尝试 data.content 结构
            if (response.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.containsKey("content")) {
                    return (String) data.get("content");
                }
            }

            // 2. 尝试 result.content 结构
            if (response.containsKey("result")) {
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                if (result != null && result.containsKey("content")) {
                    return (String) result.get("content");
                }
            }

            // 3. 尝试直接的 content 字段
            if (response.containsKey("content")) {
                return (String) response.get("content");
            }

            // 4. 尝试 text 字段
            if (response.containsKey("text")) {
                return (String) response.get("text");
            }

            // 5. 尝试 message 字段
            if (response.containsKey("message")) {
                Object message = response.get("message");
                if (message instanceof String) {
                    return (String) message;
                } else if (message instanceof Map) {
                    Map<String, Object> messageMap = (Map<String, Object>) message;
                    if (messageMap.containsKey("content")) {
                        return (String) messageMap.get("content");
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.error("通用解析失败", e);
            return null;
        }
    }

    @Override
    protected void abort(WorkInstanceEntity workInstance) {
        // AI作业中止逻辑
        Thread thread = WORK_THREAD.get(workInstance.getId());
        if (thread != null) {
            thread.interrupt();
            WORK_THREAD.remove(workInstance.getId());
        }
    }

    /**
     * 从AI响应中提取content内容（参考Python作业方式）
     */
    private String extractContentFromAiResponse(String aiResponse, StringBuilder logBuilder) {
        try {
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                return "AI未返回有效回复";
            }

            String content = aiResponse.trim();

            // 如果是JSON格式，尝试解析提取content字段
            if (content.startsWith("{") && content.endsWith("}")) {
                try {
                    Map<String, Object> jsonData = JSON.parseObject(content, Map.class);

                    // 尝试提取内容字段
                    String extractedContent = extractContentFromJson(jsonData);
                    if (extractedContent != null && !extractedContent.trim().isEmpty()) {
                        content = extractedContent;
                        logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO)
                            .append("成功从JSON响应中提取content内容 \n");
                    } else {
                        // 如果没有找到内容字段，返回格式化的JSON
                        content = JSON.toJSONString(jsonData);
                        logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO)
                            .append("未找到content字段，返回格式化JSON \n");
                    }
                } catch (Exception e) {
                    logBuilder.append(LocalDateTime.now()).append(WorkLog.SUCCESS_INFO).append("JSON解析失败，使用原始内容 \n");
                }
            }

            return content;

        } catch (Exception e) {
            log.error("提取AI响应内容失败", e);
            logBuilder.append(LocalDateTime.now()).append(WorkLog.ERROR_INFO).append("提取AI响应内容失败: ")
                .append(e.getMessage()).append(" \n");

            // 降级处理：返回原始响应
            return aiResponse != null ? aiResponse : "AI响应解析失败";
        }
    }

    /**
     * 从JSON对象中提取内容
     */
    private String extractContentFromJson(Map<String, Object> jsonData) {
        // 尝试常见的内容字段
        String[] contentFields = {"content", "text", "message", "answer", "response", "result"};

        for (String field : contentFields) {
            if (jsonData.containsKey(field)) {
                Object value = jsonData.get(field);
                if (value instanceof String) {
                    return (String) value;
                } else if (value instanceof Map) {
                    // 递归查找嵌套对象中的内容
                    String nestedContent = extractContentFromJson((Map<String, Object>) value);
                    if (nestedContent != null) {
                        return nestedContent;
                    }
                }
            }
        }

        // 尝试data字段中的内容
        if (jsonData.containsKey("data")) {
            Object data = jsonData.get("data");
            if (data instanceof Map) {
                String dataContent = extractContentFromJson((Map<String, Object>) data);
                if (dataContent != null) {
                    return dataContent;
                }
            }
        }

        // 尝试output字段中的内容（通义千问格式）
        if (jsonData.containsKey("output")) {
            Object output = jsonData.get("output");
            if (output instanceof Map) {
                String outputContent = extractContentFromJson((Map<String, Object>) output);
                if (outputContent != null) {
                    return outputContent;
                }
            }
        }

        return null;
    }
}
