package com.app.features.rbac.schema.filter;

import java.util.UUID;

import lombok.Data;

@Data
public class PermissionFilterCriteria {
    private UUID roleId;
    private UUID excludeRoleId;
}
