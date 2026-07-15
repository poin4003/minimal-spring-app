package com.app.features.user.schema.result;

import java.util.List;

import com.app.features.rbac.schema.result.PermissionResult;
import com.app.features.rbac.schema.result.RoleResult;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserDetailResult extends UserResult {

    private String loginTime;
    private String logoutTime;
    private String loginIp;
    private List<RoleResult> roles;
    private List<PermissionResult> permissions;
}
