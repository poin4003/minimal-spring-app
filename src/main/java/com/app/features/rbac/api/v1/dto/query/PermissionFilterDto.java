package com.app.features.rbac.api.v1.dto.query;

import java.util.UUID;

import com.app.features.rbac.filter.PermissionFilterCriteria;

import lombok.Data;

@Data
public class PermissionFilterDto implements PermissionFilterCriteria {

    private UUID roleId; 
}
