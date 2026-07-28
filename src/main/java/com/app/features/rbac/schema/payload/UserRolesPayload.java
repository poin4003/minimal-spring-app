package com.app.features.rbac.schema.payload;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRolesPayload {

    @NotNull(message = "{validation.rbac.userId.required}")
    private UUID userId;

    @NotEmpty(message = "{validation.rbac.roleIds.required}")
    private List<UUID> roleIds;
}
