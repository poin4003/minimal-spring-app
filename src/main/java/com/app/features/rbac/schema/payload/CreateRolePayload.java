package com.app.features.rbac.schema.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRolePayload {
    
    @NotBlank(message = "Role name must not be blank")
    private String name;

    @NotBlank(message = "Role key must not be blank")
    private String key;
}
