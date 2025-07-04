package com.isxcode.star.modules.aiconfig.mapper;

import com.isxcode.star.api.aiconfig.req.AddAiConfigReq;
import com.isxcode.star.api.aiconfig.res.PageAiConfigRes;
import com.isxcode.star.api.user.constants.UserStatus;
import com.isxcode.star.modules.aiconfig.entity.AiConfigEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AiConfigMapper {

    @Mapping(target = "status", constant = UserStatus.ENABLE)
    AiConfigEntity addAiConfigReqToAiConfigEntity(AddAiConfigReq addAiConfigReq);

    PageAiConfigRes aiConfigEntityToPageAiConfigRes(AiConfigEntity aiConfigEntity);

    List<PageAiConfigRes> aiConfigEntityToPageAiConfigResList(List<AiConfigEntity> aiConfigEntities);
}
