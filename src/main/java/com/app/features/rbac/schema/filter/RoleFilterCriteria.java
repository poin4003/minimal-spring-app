package com.app.features.rbac.schema.filter;

import java.util.UUID;

import lombok.Data;

@Data
public class RoleFilterCriteria {
    private UUID userId;
    private UUID excludeUserId;
}
