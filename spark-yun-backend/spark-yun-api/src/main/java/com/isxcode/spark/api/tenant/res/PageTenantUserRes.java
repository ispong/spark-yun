package com.isxcode.spark.api.tenant.res;

import java.util.List;
import lombok.Data;

@Data
public class PageTenantUserRes {

    private String id;

    private String username;

    private String account;

    private String status;

    private String roleCode;

    private Boolean normalAdmin;

    private List<String> roleIds;

    private String createDateTime;

    private String phone;

    private String email;

    private String userId;

    public PageTenantUserRes(String id, String account, String username, String phone, String email, String roleCode,
        Boolean normalAdmin, String status, String userId) {
        this.id = id;
        this.username = username;
        this.account = account;
        this.roleCode = roleCode;
        this.normalAdmin = normalAdmin;
        this.status = status;
        this.phone = phone;
        this.email = email;
        this.userId = userId;
    }
}
