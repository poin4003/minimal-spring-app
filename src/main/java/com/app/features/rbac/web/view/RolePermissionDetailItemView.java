package com.app.features.rbac.web.view;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RolePermissionDetailItemView {

    private final UUID permissionId;
    private final String key;
    private final String name;
    private final String actionPath;
    private final String actionLabel;
    private final String actionButtonClass;
}
