package com.isxcode.spark.modules.authorization.service;

import com.isxcode.spark.api.authorization.req.DeleteOrgReq;
import com.isxcode.spark.api.authorization.req.SaveOrgReq;
import com.isxcode.spark.api.authorization.res.OrgRes;
import com.isxcode.spark.backend.api.base.exceptions.IsxAppException;
import com.isxcode.spark.common.security.ContextHolder;
import com.isxcode.spark.security.authorization.OrgEntity;
import com.isxcode.spark.security.authorization.OrgMemberEntity;
import com.isxcode.spark.security.authorization.OrgMemberRepository;
import com.isxcode.spark.security.authorization.OrgRepository;
import com.isxcode.spark.security.authorization.OrgRoleEntity;
import com.isxcode.spark.security.authorization.OrgRoleRepository;
import com.isxcode.spark.security.authorization.RoleRepository;
import com.isxcode.spark.security.user.TenantUserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class OrgBizService {

    private final OrgRepository orgRepository;

    private final OrgMemberRepository orgMemberRepository;

    private final OrgRoleRepository orgRoleRepository;

    private final RoleRepository roleRepository;

    private final TenantUserRepository tenantUserRepository;

    public void saveOrg(SaveOrgReq request) {

        String tenantId = requireTenantId();
        OrgEntity org = Strings.isEmpty(request.getId()) ? new OrgEntity() : getCurrentTenantOrg(request.getId());
        orgRepository.findByTenantIdAndName(tenantId, request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(org.getId())) {
                throw new IsxAppException("同一租户下组织名称不能重复");
            }
        });
        validateParent(org.getId(), request.getParentId());
        org.setTenantId(tenantId);
        org.setName(request.getName());
        org.setParentId(request.getParentId());
        OrgEntity savedOrg = orgRepository.save(org);

        orgMemberRepository.deleteAllByTenantIdAndOrgId(tenantId, savedOrg.getId());
        if (request.getUserIds() != null) {
            request.getUserIds().stream().distinct().forEach(userId -> {
                tenantUserRepository.findByTenantIdAndUserId(tenantId, userId)
                    .orElseThrow(() -> new IsxAppException("组织成员不属于当前租户"));
                OrgMemberEntity member = new OrgMemberEntity();
                member.setTenantId(tenantId);
                member.setOrgId(savedOrg.getId());
                member.setUserId(userId);
                orgMemberRepository.save(member);
            });
        }

        orgRoleRepository.deleteAllByTenantIdAndOrgId(tenantId, savedOrg.getId());
        if (request.getRoleIds() != null) {
            request.getRoleIds().stream().distinct().forEach(roleId -> {
                roleRepository.findById(roleId).filter(role -> tenantId.equals(role.getTenantId()))
                    .orElseThrow(() -> new IsxAppException("组织角色不属于当前租户"));
                OrgRoleEntity orgRole = new OrgRoleEntity();
                orgRole.setTenantId(tenantId);
                orgRole.setOrgId(savedOrg.getId());
                orgRole.setRoleId(roleId);
                orgRoleRepository.save(orgRole);
            });
        }
    }

    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public List<OrgRes> listOrg() {

        String tenantId = requireTenantId();
        return orgRepository.findAllByTenantIdOrderByCreateDateTimeAsc(tenantId).stream()
            .map(org -> OrgRes.builder().id(org.getId()).parentId(org.getParentId()).name(org.getName())
                .userIds(orgMemberRepository.findAllByTenantIdAndOrgId(tenantId, org.getId()).stream()
                    .map(OrgMemberEntity::getUserId).toList())
                .roleIds(orgRoleRepository.findAllByTenantIdAndOrgId(tenantId, org.getId()).stream()
                    .map(OrgRoleEntity::getRoleId).toList())
                .build())
            .toList();
    }

    public void deleteOrg(DeleteOrgReq request) {

        String tenantId = requireTenantId();
        OrgEntity org = getCurrentTenantOrg(request.getOrgId());
        orgRepository.findAllByTenantIdOrderByCreateDateTimeAsc(tenantId).stream()
            .filter(child -> org.getId().equals(child.getParentId())).forEach(child -> {
                child.setParentId(org.getParentId());
                orgRepository.save(child);
            });
        orgMemberRepository.deleteAllByTenantIdAndOrgId(tenantId, org.getId());
        orgRoleRepository.deleteAllByTenantIdAndOrgId(tenantId, org.getId());
        orgRepository.delete(org);
    }

    private void validateParent(String orgId, String parentId) {

        if (Strings.isEmpty(parentId)) {
            return;
        }
        OrgEntity parent = getCurrentTenantOrg(parentId);
        Set<String> visited = new HashSet<>();
        while (parent != null && visited.add(parent.getId())) {
            if (parent.getId().equals(orgId)) {
                throw new IsxAppException("组织层级不能形成循环");
            }
            parent = Strings.isEmpty(parent.getParentId()) ? null : getCurrentTenantOrg(parent.getParentId());
        }
    }

    private OrgEntity getCurrentTenantOrg(String orgId) {

        OrgEntity org = orgRepository.findById(orgId).orElseThrow(() -> new IsxAppException("组织不存在"));
        if (!requireTenantId().equals(org.getTenantId())) {
            throw new IsxAppException("无权操作其他租户组织");
        }
        return org;
    }

    private String requireTenantId() {

        if (Strings.isEmpty(ContextHolder.getTenantId())) {
            throw new IsxAppException("租户id丢失");
        }
        return ContextHolder.getTenantId();
    }
}
