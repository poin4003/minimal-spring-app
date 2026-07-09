package com.app.features.rbac.api.v1.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRoleDto {
    
    @NotBlank(message = "Role name must not be blank")
    private String name;

    @NotBlank(message = "Role key must not be blank")
    private String key;
}
