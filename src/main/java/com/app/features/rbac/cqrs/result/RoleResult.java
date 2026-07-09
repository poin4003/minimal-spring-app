package com.app.features.rbac.cqrs.result;

import lombok.Data;

@Data
public class RoleResult {

    private String id;

    private String name;

    private String key;
}
