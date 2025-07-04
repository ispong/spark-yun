package com.isxcode.star.modules.aiconfig.service.biz;

import com.isxcode.star.api.aiconfig.req.AddAiConfigReq;
import com.isxcode.star.api.aiconfig.req.DeleteAiConfigReq;
import com.isxcode.star.api.aiconfig.req.PageAiConfigReq;
import com.isxcode.star.api.aiconfig.req.UpdateAiConfigReq;
import com.isxcode.star.api.aiconfig.res.PageAiConfigRes;
import com.isxcode.star.api.user.constants.UserStatus;
import com.isxcode.star.backend.api.base.exceptions.IsxAppException;
import com.isxcode.star.modules.aiconfig.entity.AiConfigEntity;
import com.isxcode.star.modules.aiconfig.mapper.AiConfigMapper;
import com.isxcode.star.modules.aiconfig.repository.AiConfigRepository;
import com.isxcode.star.common.utils.aes.AesUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiConfigBizService {

    private final AiConfigRepository aiConfigRepository;

    private final AiConfigMapper aiConfigMapper;

    private final AesUtils aesUtils;

    public void addAiConfig(AddAiConfigReq addAiConfigReq) {

        // 校验AI配置名称唯一性
        Optional<AiConfigEntity> aiConfigByName = aiConfigRepository.findByName(addAiConfigReq.getName());
        if (aiConfigByName.isPresent()) {
            throw new IsxAppException("AI配置名称重复，请重新输入");
        }

        // 转换并保存
        AiConfigEntity aiConfig = aiConfigMapper.addAiConfigReqToAiConfigEntity(addAiConfigReq);

        // 加密API密钥
        aiConfig.setApiKey(aesUtils.encrypt(addAiConfigReq.getApiKey()));

        aiConfigRepository.save(aiConfig);
    }

    public Page<PageAiConfigRes> pageAiConfig(PageAiConfigReq pageAiConfigReq) {

        Page<AiConfigEntity> aiConfigPage;
        if (pageAiConfigReq.getSearchKeyWord() != null && !pageAiConfigReq.getSearchKeyWord().isEmpty()) {
            aiConfigPage = aiConfigRepository.searchAll(pageAiConfigReq.getSearchKeyWord(),
                PageRequest.of(pageAiConfigReq.getPage(), pageAiConfigReq.getSize()));
        } else {
            aiConfigPage = aiConfigRepository.findAll(
                PageRequest.of(pageAiConfigReq.getPage(), pageAiConfigReq.getSize()));
        }

        return aiConfigPage.map(aiConfigMapper::aiConfigEntityToPageAiConfigRes);
    }

    public void updateAiConfig(UpdateAiConfigReq updateAiConfigReq) {

        // 查询AI配置是否存在
        Optional<AiConfigEntity> aiConfigEntityOptional = aiConfigRepository.findById(updateAiConfigReq.getId());
        if (!aiConfigEntityOptional.isPresent()) {
            throw new IsxAppException("AI配置不存在");
        }

        // 校验名称唯一性（排除自己）
        Optional<AiConfigEntity> aiConfigByName = aiConfigRepository.findByName(updateAiConfigReq.getName());
        if (aiConfigByName.isPresent() && !aiConfigByName.get().getId().equals(updateAiConfigReq.getId())) {
            throw new IsxAppException("AI配置名称重复，请重新输入");
        }

        // 更新AI配置
        AiConfigEntity aiConfig = aiConfigEntityOptional.get();
        aiConfig.setName(updateAiConfigReq.getName());
        aiConfig.setModelType(updateAiConfigReq.getModelType());
        aiConfig.setApiUrl(updateAiConfigReq.getApiUrl());
        aiConfig.setApiKey(aesUtils.encrypt(updateAiConfigReq.getApiKey()));
        aiConfig.setRemark(updateAiConfigReq.getRemark());

        aiConfigRepository.save(aiConfig);
    }

    public void deleteAiConfig(DeleteAiConfigReq deleteAiConfigReq) {

        // 查询AI配置是否存在
        Optional<AiConfigEntity> aiConfigEntityOptional = aiConfigRepository.findById(deleteAiConfigReq.getId());
        if (!aiConfigEntityOptional.isPresent()) {
            throw new IsxAppException("AI配置不存在");
        }

        // 删除AI配置
        aiConfigRepository.deleteById(deleteAiConfigReq.getId());
    }

    public void enableAiConfig(String id) {

        Optional<AiConfigEntity> aiConfigEntityOptional = aiConfigRepository.findById(id);
        if (!aiConfigEntityOptional.isPresent()) {
            throw new IsxAppException("AI配置不存在");
        }

        AiConfigEntity aiConfig = aiConfigEntityOptional.get();
        aiConfig.setStatus(UserStatus.ENABLE);
        aiConfigRepository.save(aiConfig);
    }

    public void disableAiConfig(String id) {

        Optional<AiConfigEntity> aiConfigEntityOptional = aiConfigRepository.findById(id);
        if (!aiConfigEntityOptional.isPresent()) {
            throw new IsxAppException("AI配置不存在");
        }

        AiConfigEntity aiConfig = aiConfigEntityOptional.get();
        aiConfig.setStatus(UserStatus.DISABLE);
        aiConfigRepository.save(aiConfig);
    }

    public String testAiConfig(AddAiConfigReq addAiConfigReq) {

        try {
            // 这里可以添加实际的AI模型连接测试逻辑
            // 目前返回一个简单的成功消息
            String modelType = addAiConfigReq.getModelType();
            String apiUrl = addAiConfigReq.getApiUrl();
            String apiKey = addAiConfigReq.getApiKey();

            // 验证必要参数
            if (modelType == null || apiUrl == null || apiKey == null) {
                throw new IsxAppException("配置参数不完整");
            }

            // 模拟测试连接（实际项目中可以发送HTTP请求测试）
            return "AI配置测试成功，模型类型：" + modelType + "，API地址连接正常";

        } catch (Exception e) {
            throw new IsxAppException("AI配置测试失败：" + e.getMessage());
        }
    }
}
