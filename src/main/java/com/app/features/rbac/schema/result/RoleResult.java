package com.app.features.rbac.schema.result;

import lombok.Data;

@Data
public class RoleResult {

    private String id;

    private String name;

    private String key;

    private String createdAt;

    private String updatedAt;
}
