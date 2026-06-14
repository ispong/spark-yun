package com.isxcode.spark.modules.authorization.controller;

import com.isxcode.spark.api.authorization.req.DeleteOrgReq;
import com.isxcode.spark.api.authorization.req.SaveOrgReq;
import com.isxcode.spark.api.authorization.res.OrgRes;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.common.annotations.successResponse.SuccessResponse;
import com.isxcode.spark.common.userlog.UserLog;
import com.isxcode.spark.modules.authorization.service.OrgBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "组织架构")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/orgs")
@Secured({RoleType.TENANT_SUPER_ADMIN, RoleType.TENANT_ADMIN})
public class OrgController {

    private final OrgBizService orgBizService;

    @Operation(summary = "保存组织")
    @PostMapping("/save")
    @UserLog
    @SuccessResponse("保存成功")
    public void saveOrg(@Valid @RequestBody SaveOrgReq request) {

        orgBizService.saveOrg(request);
    }

    @Operation(summary = "查询组织")
    @PostMapping("/list")
    @SuccessResponse("查询成功")
    public List<OrgRes> listOrg() {

        return orgBizService.listOrg();
    }

    @Operation(summary = "删除组织")
    @PostMapping("/delete")
    @UserLog
    @SuccessResponse("删除成功")
    public void deleteOrg(@Valid @RequestBody DeleteOrgReq request) {

        orgBizService.deleteOrg(request);
    }
}
