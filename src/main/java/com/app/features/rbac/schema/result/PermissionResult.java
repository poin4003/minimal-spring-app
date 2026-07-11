package com.app.features.rbac.schema.result;

import java.util.UUID;

import lombok.Data;

@Data
public class PermissionResult {

    private UUID id;

    private String key;

    private String name; 
}
