package com.app.features.rbac.schema.result;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RoleResult {

    private String id;

    private String name;

    private String key;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
