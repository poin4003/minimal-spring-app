package com.app.features.rbac.schema.payload;

import lombok.Data;

@Data
public class UpdateRolePayload {

    private String name;

    private String key;
}
