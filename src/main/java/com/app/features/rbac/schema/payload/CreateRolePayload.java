package com.app.features.rbac.schema.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRolePayload {
    
    @NotBlank(message = "{validation.rbac.roleName.required}")
    private String name;

    @NotBlank(message = "{validation.rbac.roleKey.required}")
    private String key;
}
