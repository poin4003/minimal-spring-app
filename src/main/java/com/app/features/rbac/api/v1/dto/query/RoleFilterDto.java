package com.app.features.rbac.api.v1.dto.query;

import java.util.UUID;

import com.app.features.rbac.filter.RoleFilterCriteria;

import lombok.Data;

@Data
public class RoleFilterDto implements RoleFilterCriteria {
    
    private UUID userId;
}
