package com.app.features.rbac.web.view;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoleDetailModalView {

    private final String id;
    private final UUID roleId;
    private final String title;
    private final String roleKey;
    private final String roleName;
    private final List<RolePermissionDetailItemView> assignedPermissions;
    private final List<RolePermissionDetailItemView> availablePermissions;
}
