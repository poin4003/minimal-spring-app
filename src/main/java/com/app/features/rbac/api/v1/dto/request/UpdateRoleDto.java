package com.app.features.rbac.api.v1.dto.request;

import lombok.Data;

@Data
public class UpdateRoleDto {

    private String name;

    private String key;
}
