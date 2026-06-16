package com.isxcode.spark.modules.ai.service;

import com.isxcode.spark.api.ai.constants.AiConfigStatus;
import com.isxcode.spark.api.ai.constants.AiProviderType;
import com.isxcode.spark.api.ai.req.AiChatReq;
import com.isxcode.spark.api.ai.req.DeleteAiChatSessionReq;
import com.isxcode.spark.api.ai.req.DeleteAiConfigReq;
import com.isxcode.spark.api.ai.req.DeleteAiPromptReq;
import com.isxcode.spark.api.ai.req.PageAiConfigReq;
import com.isxcode.spark.api.ai.req.SaveAiChatSessionReq;
import com.isxcode.spark.api.ai.req.SaveAiConfigReq;
import com.isxcode.spark.api.ai.req.SaveAiPromptReq;
import com.isxcode.spark.api.ai.req.TestAiConfigReq;
import com.isxcode.spark.api.ai.res.AiChatFileRes;
import com.isxcode.spark.api.ai.res.AiChatRes;
import com.isxcode.spark.api.ai.res.AiChatSessionRes;
import com.isxcode.spark.api.ai.res.AiConfigRes;
import com.isxcode.spark.api.ai.res.AiPromptRes;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.common.security.ContextHolder;
import com.isxcode.spark.modules.ai.entity.AiChatSessionEntity;
import com.isxcode.spark.modules.ai.entity.AiConfigEntity;
import com.isxcode.spark.modules.ai.entity.AiPromptEntity;
import com.isxcode.spark.modules.ai.repository.AiChatSessionRepository;
import com.isxcode.spark.modules.ai.repository.AiConfigRepository;
import com.isxcode.spark.modules.ai.repository.AiPromptRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class AiConfigBizService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final double DEFAULT_TEMPERATURE = 0.7;

    private static final int DEFAULT_MAX_TOKENS = 2000;

    private static final long MAX_CHAT_FILE_SIZE = 10 * 1024 * 1024;

    private static final int MAX_EXTRACTED_FILE_CHARS = 20000;

    private final AiConfigRepository aiConfigRepository;

    private final AiChatSessionRepository aiChatSessionRepository;

    private final AiPromptRepository aiPromptRepository;

    private final ObjectMapper objectMapper;

    public void saveConfig(SaveAiConfigReq request) {

        String tenantId = requireTenantId();
        AiConfigEntity aiConfig;
        if (Strings.isEmpty(request.getId())) {
            aiConfig = new AiConfigEntity();
        } else {
            aiConfig = getCurrentTenantConfig(request.getId());
        }

        aiConfigRepository.findByTenantIdAndName(tenantId, request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(aiConfig.getId())) {
                throw new IsxAppException("同一租户下智能配置名称不能重复");
            }
        });

        aiConfig.setTenantId(tenantId);
        aiConfig.setName(request.getName().trim());
        aiConfig.setProviderType(normalizeProvider(request.getProviderType()));
        aiConfig.setBaseUrl(defaultBaseUrl(aiConfig.getProviderType(), request.getBaseUrl()));
        if (!Strings.isEmpty(request.getApiKey())) {
            aiConfig.setApiKey(request.getApiKey().trim());
        }
        aiConfig.setModelName(request.getModelName().trim());
        aiConfig.setTemperature(request.getTemperature() == null ? DEFAULT_TEMPERATURE : request.getTemperature());
        aiConfig.setMaxTokens(request.getMaxTokens() == null ? DEFAULT_MAX_TOKENS : request.getMaxTokens());
        aiConfig.setStatus(Strings.isEmpty(request.getStatus()) ? AiConfigStatus.ENABLE : request.getStatus());
        aiConfig.setRemark(request.getRemark());
        validateConfig(aiConfig);
        aiConfigRepository.save(aiConfig);
    }

    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public Page<AiConfigRes> pageConfig(PageAiConfigReq request) {

        String keyword = request.getSearchKeyWord() == null ? "" : request.getSearchKeyWord();
        return aiConfigRepository
            .search(requireTenantId(), keyword, PageRequest.of(request.getPage(), request.getPageSize()))
            .map(this::toConfigRes);
    }

    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public List<AiConfigRes> listEnabledConfig() {

        return aiConfigRepository
            .findAllByTenantIdAndStatusOrderByCreateDateTimeDesc(requireTenantId(), AiConfigStatus.ENABLE).stream()
            .map(this::toConfigRes).toList();
    }

    public void deleteConfig(DeleteAiConfigReq request) {

        aiConfigRepository.delete(getCurrentTenantConfig(request.getId()));
    }

    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public List<AiChatSessionRes> listChatSessions() {

        return aiChatSessionRepository
            .findAllByTenantIdAndUserIdOrderByLastModifiedDateTimeDesc(requireTenantId(), requireUserId()).stream()
            .map(this::toChatSessionRes).toList();
    }

    public AiChatSessionRes saveChatSession(SaveAiChatSessionReq request) {

        getCurrentTenantConfig(request.getConfigId());
        AiChatSessionEntity session = Strings.isEmpty(request.getId()) ? new AiChatSessionEntity()
            : aiChatSessionRepository.findById(request.getId()).orElseGet(() -> {
                AiChatSessionEntity newSession = new AiChatSessionEntity();
                newSession.setId(request.getId());
                return newSession;
            });

        if (session.getTenantId() != null && !requireTenantId().equals(session.getTenantId())) {
            throw new IsxAppException("无权操作其他租户会话");
        }
        if (session.getUserId() != null && !requireUserId().equals(session.getUserId())) {
            throw new IsxAppException("无权操作其他用户会话");
        }

        session.setTenantId(requireTenantId());
        session.setUserId(requireUserId());
        session.setConfigId(request.getConfigId());
        session.setTitle(resolveSessionTitle(request.getTitle()));
        session.setMessagesJson(toMessagesJson(request.getMessages()));
        return toChatSessionRes(aiChatSessionRepository.save(session));
    }

    public void deleteChatSession(DeleteAiChatSessionReq request) {

        AiChatSessionEntity session = getCurrentUserSession(request.getId());
        aiChatSessionRepository.delete(session);
    }

    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public List<AiPromptRes> listPrompts() {

        return aiPromptRepository.findAllByTenantIdOrderByLastModifiedDateTimeDesc(requireTenantId()).stream()
            .map(this::toPromptRes).toList();
    }

    public AiPromptRes savePrompt(SaveAiPromptReq request) {

        String tenantId = requireTenantId();
        AiPromptEntity prompt = Strings.isEmpty(request.getId()) ? new AiPromptEntity() : getCurrentTenantPrompt(request.getId());
        String promptName = request.getName().trim();

        aiPromptRepository.findByTenantIdAndName(tenantId, promptName).ifPresent(existing -> {
            if (!existing.getId().equals(prompt.getId())) {
                throw new IsxAppException("同一租户下提示词名称不能重复");
            }
        });

        prompt.setTenantId(tenantId);
        prompt.setName(promptName);
        prompt.setContent(request.getContent().trim());
        return toPromptRes(aiPromptRepository.save(prompt));
    }

    public void deletePrompt(DeleteAiPromptReq request) {

        aiPromptRepository.delete(getCurrentTenantPrompt(request.getId()));
    }

    public AiChatFileRes parseChatFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IsxAppException("请选择文件");
        }
        if (file.getSize() > MAX_CHAT_FILE_SIZE) {
            throw new IsxAppException("单个文件不能超过10MB");
        }

        String fileName = file.getOriginalFilename() == null ? "未命名文件" : file.getOriginalFilename();
        String contentType = file.getContentType();
        String content;
        try {
            byte[] bytes = file.getBytes();
            content = extractFileContent(fileName, bytes);
        } catch (Exception exception) {
            content = "文件内容暂无法自动解析，请根据文件名、类型和业务上下文判断。";
        }

        return AiChatFileRes.builder().name(fileName).contentType(contentType).size(file.getSize())
            .content(limitExtractedContent(content)).build();
    }

    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public void testConfig(TestAiConfigReq request) {

        AiConfigEntity config = getCurrentTenantConfig(request.getId());
        if (!AiConfigStatus.ENABLE.equals(config.getStatus())) {
            throw new IsxAppException("智能配置已禁用");
        }
        validateConfig(config);

        try {
            buildChatModel(config).call(new Prompt(List.of(new UserMessage("Reply with OK only."))));
        } catch (Exception exception) {
            throw new IsxAppException("智能配置测试失败：" + exception.getMessage());
        }
    }

    public AiChatRes chat(AiChatReq request) {

        AiConfigEntity config = getCurrentTenantConfig(request.getConfigId());
        if (!AiConfigStatus.ENABLE.equals(config.getStatus())) {
            throw new IsxAppException("智能配置已禁用");
        }
        validateConfig(config);
        List<Message> messages = toSpringAiMessages(request);

        try {
            ChatResponse response = buildChatModel(config).call(new Prompt(messages));
            String content = extractContent(response);
            return AiChatRes.builder().content(content).build();
        } catch (Exception exception) {
            throw new IsxAppException("AI对话失败：" + exception.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public StreamingResponseBody streamChat(AiChatReq request) {

        AiConfigEntity config = getCurrentTenantConfig(request.getConfigId());
        if (!AiConfigStatus.ENABLE.equals(config.getStatus())) {
            throw new IsxAppException("智能配置已禁用");
        }
        validateConfig(config);
        Flux<ChatResponse> responseFlux = buildChatModel(config).stream(new Prompt(toSpringAiMessages(request)));

        return outputStream -> {
            try {
                writeSse(outputStream, "start", Map.of());
                for (ChatResponse response : responseFlux.toIterable()) {
                    String content = extractContent(response);
                    if (!Strings.isEmpty(content)) {
                        writeSse(outputStream, "message", Map.of("content", content));
                    }
                }
                writeSse(outputStream, "done", Map.of());
            } catch (IOException exception) {
                // The browser can close the stream when the user stops generation.
            } catch (Exception exception) {
                try {
                    writeSse(outputStream, "error", Map.of("message", "AI对话失败：" + exception.getMessage()));
                } catch (IOException ignored) {
                    // The client may have disconnected before the error event is written.
                }
            }
        };
    }

    private List<Message> toSpringAiMessages(AiChatReq request) {

        List<Message> messages = request.getMessages().stream()
            .filter(message -> !Strings.isEmpty(message.getContent())).map(this::toSpringAiMessage).toList();
        if (messages.isEmpty()) {
            throw new IsxAppException("请输入对话内容");
        }
        return messages;
    }

    private Message toSpringAiMessage(AiChatReq.AiChatMessageReq message) {

        String role = message.getRole() == null ? "user" : message.getRole();
        return switch (role) {
            case "system" -> new SystemMessage(message.getContent());
            case "assistant" -> new AssistantMessage(message.getContent());
            default -> new UserMessage(message.getContent());
        };
    }

    private OpenAiChatModel buildChatModel(AiConfigEntity config) {

        OpenAiApi openAiApi = OpenAiApi.builder().baseUrl(config.getBaseUrl()).apiKey(resolveApiKey(config)).build();
        OpenAiChatOptions options = OpenAiChatOptions.builder().model(config.getModelName())
            .temperature(config.getTemperature()).maxTokens(config.getMaxTokens()).build();
        return OpenAiChatModel.builder().openAiApi(openAiApi).defaultOptions(options).build();
    }

    private String extractContent(ChatResponse response) {

        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        return response.getResult().getOutput().getText();
    }

    private void writeSse(OutputStream outputStream, String event, Map<String, String> data) throws IOException {

        outputStream.write(("event: " + event + "\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("data: " + toJson(data) + "\n\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private String toJson(Map<String, String> data) {

        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new IsxAppException("AI响应序列化失败：" + exception.getMessage());
        }
    }

    private String resolveApiKey(AiConfigEntity config) {

        if (AiProviderType.OLLAMA.equals(config.getProviderType()) && Strings.isEmpty(config.getApiKey())) {
            return "ollama";
        }
        return config.getApiKey();
    }

    private void validateConfig(AiConfigEntity config) {

        if (Strings.isEmpty(config.getBaseUrl())) {
            throw new IsxAppException("接口地址不能为空");
        }
        if (Strings.isEmpty(config.getModelName())) {
            throw new IsxAppException("模型不能为空");
        }
        if (!AiProviderType.OLLAMA.equals(config.getProviderType()) && Strings.isEmpty(config.getApiKey())) {
            throw new IsxAppException("API Key不能为空");
        }
        if (config.getTemperature() == null || config.getTemperature() < 0 || config.getTemperature() > 2) {
            throw new IsxAppException("温度需要在0到2之间");
        }
        if (config.getMaxTokens() == null || config.getMaxTokens() <= 0) {
            throw new IsxAppException("最大Token需要大于0");
        }
    }

    private String normalizeProvider(String providerType) {

        return switch (providerType) {
            case AiProviderType.OPENAI, AiProviderType.DEEPSEEK, AiProviderType.DASHSCOPE, AiProviderType.OLLAMA,
                AiProviderType.OPENAI_COMPATIBLE -> providerType;
            default -> throw new IsxAppException("不支持的AI供应商");
        };
    }

    private String defaultBaseUrl(String providerType, String baseUrl) {

        if (!Strings.isEmpty(baseUrl)) {
            return baseUrl.trim();
        }
        return switch (providerType) {
            case AiProviderType.OPENAI -> "https://api.openai.com";
            case AiProviderType.DEEPSEEK -> "https://api.deepseek.com";
            case AiProviderType.DASHSCOPE -> "https://dashscope.aliyuncs.com/compatible-mode";
            case AiProviderType.OLLAMA -> "http://localhost:11434";
            default -> throw new IsxAppException("自定义AI供应商需要填写接口地址");
        };
    }

    private AiConfigEntity getCurrentTenantConfig(String id) {

        AiConfigEntity config = aiConfigRepository.findById(id).orElseThrow(() -> new IsxAppException("智能配置不存在"));
        if (!requireTenantId().equals(config.getTenantId())) {
            throw new IsxAppException("无权操作其他租户智能配置");
        }
        return config;
    }

    private AiConfigRes toConfigRes(AiConfigEntity config) {

        return AiConfigRes.builder().id(config.getId()).name(config.getName()).providerType(config.getProviderType())
            .baseUrl(config.getBaseUrl()).modelName(config.getModelName()).temperature(config.getTemperature())
            .maxTokens(config.getMaxTokens()).status(config.getStatus()).remark(config.getRemark())
            .createDateTime(
                config.getCreateDateTime() == null ? null : config.getCreateDateTime().format(DATE_TIME_FORMATTER))
            .build();
    }

    private AiChatSessionEntity getCurrentUserSession(String id) {

        AiChatSessionEntity session =
            aiChatSessionRepository.findById(id).orElseThrow(() -> new IsxAppException("会话不存在"));
        if (!requireTenantId().equals(session.getTenantId()) || !requireUserId().equals(session.getUserId())) {
            throw new IsxAppException("无权操作其他用户会话");
        }
        return session;
    }

    private AiPromptEntity getCurrentTenantPrompt(String id) {

        AiPromptEntity prompt = aiPromptRepository.findById(id).orElseThrow(() -> new IsxAppException("提示词不存在"));
        if (!requireTenantId().equals(prompt.getTenantId())) {
            throw new IsxAppException("无权操作其他租户提示词");
        }
        return prompt;
    }

    private AiChatSessionRes toChatSessionRes(AiChatSessionEntity session) {

        return AiChatSessionRes.builder().id(session.getId()).configId(session.getConfigId()).title(session.getTitle())
            .updatedAt(session.getLastModifiedDateTime() == null ? null
                : session.getLastModifiedDateTime().format(DATE_TIME_FORMATTER))
            .messages(parseMessages(session.getMessagesJson())).build();
    }

    private AiPromptRes toPromptRes(AiPromptEntity prompt) {

        return AiPromptRes.builder().id(prompt.getId()).name(prompt.getName()).content(prompt.getContent())
            .updatedAt(prompt.getLastModifiedDateTime() == null ? null
                : prompt.getLastModifiedDateTime().format(DATE_TIME_FORMATTER))
            .build();
    }

    private String extractFileContent(String fileName, byte[] bytes) throws Exception {

        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        if (lowerFileName.endsWith(".docx")) {
            return extractDocx(bytes);
        }
        if (lowerFileName.endsWith(".pdf")) {
            return extractPdf(bytes);
        }
        if (lowerFileName.endsWith(".xlsx") || lowerFileName.endsWith(".xls")) {
            return extractWorkbook(bytes);
        }
        if (isTextLikeFile(lowerFileName)) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return "文件内容暂无法自动解析，请根据文件名、类型和业务上下文判断。";
    }

    private String extractDocx(byte[] bytes) throws IOException {

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder content = new StringBuilder();
            document.getParagraphs().forEach(paragraph -> {
                if (!Strings.isEmpty(paragraph.getText())) {
                    content.append(paragraph.getText()).append('\n');
                }
            });
            document.getTables().forEach(table -> table.getRows().forEach(row -> {
                row.getTableCells().forEach(cell -> content.append(cell.getText()).append('\t'));
                content.append('\n');
            }));
            return content.toString();
        }
    }

    private String extractPdf(byte[] bytes) throws IOException {

        try (org.apache.pdfbox.pdmodel.PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractWorkbook(byte[] bytes) throws IOException {

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            DataFormatter formatter = new DataFormatter();
            StringBuilder content = new StringBuilder();
            workbook.forEach(sheet -> {
                content.append("Sheet: ").append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    row.forEach(cell -> content.append(formatter.formatCellValue(cell)).append('\t'));
                    content.append('\n');
                }
            });
            return content.toString();
        }
    }

    private boolean isTextLikeFile(String lowerFileName) {

        return lowerFileName.endsWith(".txt") || lowerFileName.endsWith(".md") || lowerFileName.endsWith(".csv")
            || lowerFileName.endsWith(".json") || lowerFileName.endsWith(".xml") || lowerFileName.endsWith(".html")
            || lowerFileName.endsWith(".htm") || lowerFileName.endsWith(".sql") || lowerFileName.endsWith(".log")
            || lowerFileName.endsWith(".yml") || lowerFileName.endsWith(".yaml")
            || lowerFileName.endsWith(".properties") || lowerFileName.endsWith(".java")
            || lowerFileName.endsWith(".py") || lowerFileName.endsWith(".js") || lowerFileName.endsWith(".ts")
            || lowerFileName.endsWith(".vue") || lowerFileName.endsWith(".css") || lowerFileName.endsWith(".scss")
            || lowerFileName.endsWith(".sh") || lowerFileName.endsWith(".bat");
    }

    private String limitExtractedContent(String content) {

        if (content == null) {
            return "";
        }
        if (content.length() <= MAX_EXTRACTED_FILE_CHARS) {
            return content;
        }
        return content.substring(0, MAX_EXTRACTED_FILE_CHARS) + "\n\n[文件内容过长，已截取前"
            + MAX_EXTRACTED_FILE_CHARS + "个字符]";
    }

    private String resolveSessionTitle(String title) {

        String trimmedTitle = title == null ? "" : title.trim();
        if (trimmedTitle.length() > 200) {
            return trimmedTitle.substring(0, 200);
        }
        return trimmedTitle;
    }

    private String toMessagesJson(List<AiChatReq.AiChatMessageReq> messages) {

        try {
            return objectMapper.writeValueAsString(messages);
        } catch (JsonProcessingException exception) {
            throw new IsxAppException("会话消息序列化失败：" + exception.getMessage());
        }
    }

    private List<AiChatReq.AiChatMessageReq> parseMessages(String messagesJson) {

        try {
            return objectMapper.readValue(messagesJson, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IsxAppException("会话消息解析失败：" + exception.getMessage());
        }
    }

    private String requireTenantId() {

        if (Strings.isEmpty(ContextHolder.getTenantId())) {
            throw new IsxAppException("租户id丢失");
        }
        return ContextHolder.getTenantId();
    }

    private String requireUserId() {

        if (Strings.isEmpty(ContextHolder.getUserId())) {
            throw new IsxAppException("用户id丢失");
        }
        return ContextHolder.getUserId();
    }
}
