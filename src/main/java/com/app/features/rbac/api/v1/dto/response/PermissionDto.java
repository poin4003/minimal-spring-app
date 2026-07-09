package com.app.features.rbac.api.v1.dto.response;

import java.util.UUID;

import lombok.Data;

@Data
public class PermissionDto {

    private UUID id;

    private String key;

    private String name; 
}
