package com.isxcode.spark.modules.authorization.controller;

import com.isxcode.spark.api.authorization.req.DeleteRoleReq;
import com.isxcode.spark.api.authorization.req.PageRoleReq;
import com.isxcode.spark.api.authorization.req.SaveRoleReq;
import com.isxcode.spark.api.authorization.res.PermissionCatalogRes;
import com.isxcode.spark.api.authorization.res.RoleRes;
import com.isxcode.spark.api.user.constants.RoleType;
import com.isxcode.spark.common.annotations.successResponse.SuccessResponse;
import com.isxcode.spark.common.userlog.UserLog;
import com.isxcode.spark.modules.authorization.service.RoleBizService;
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

@Tag(name = "role-permission", description = "角色权限")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/roles")
@Secured({RoleType.TENANT_SUPER_ADMIN, RoleType.TENANT_ADMIN})
public class RoleController {

    private final RoleBizService roleBizService;

    @Operation(summary = "保存角色")
    @PostMapping("/save")
    @UserLog
    @SuccessResponse("保存成功")
    public void saveRole(@Valid @RequestBody SaveRoleReq request) {

        roleBizService.saveRole(request);
    }

    @Operation(summary = "分页查询角色")
    @PostMapping("/page")
    @SuccessResponse("查询成功")
    public Page<RoleRes> pageRole(@Valid @RequestBody PageRoleReq request) {

        return roleBizService.pageRole(request);
    }

    @Operation(summary = "查询可用角色")
    @PostMapping("/list")
    @SuccessResponse("查询成功")
    public List<RoleRes> listRole() {

        return roleBizService.listRole();
    }

    @Operation(summary = "删除角色")
    @PostMapping("/delete")
    @UserLog
    @SuccessResponse("删除成功")
    public void deleteRole(@Valid @RequestBody DeleteRoleReq request) {

        roleBizService.deleteRole(request);
    }

    @Operation(summary = "查询权限目录")
    @PostMapping("/permissions")
    @SuccessResponse("查询成功")
    public PermissionCatalogRes permissionCatalog() {

        return roleBizService.permissionCatalog();
    }
}
