package com.app.features.rbac.schema.payload;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RolePermissionsPayload {

    @NotNull(message = "{validation.rbac.roleId.required}")
    private UUID roleId;

    @NotEmpty(message = "{validation.rbac.permissionIds.required}")
    private List<UUID> permIds;
}
