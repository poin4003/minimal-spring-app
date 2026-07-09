package com.app.features.rbac.api.v1.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RolePermissionsDto {

    @NotNull(message = "Require role id")
    private UUID roleId;

    @NotEmpty(message = "List permission id must not be empty")
    private List<UUID> permIds;
}
