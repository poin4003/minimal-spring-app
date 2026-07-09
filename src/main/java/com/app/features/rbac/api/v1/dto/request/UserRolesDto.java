package com.app.features.rbac.api.v1.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRolesDto {

    @NotNull(message = "Require user id")
    private UUID userId;

    @NotEmpty(message = "List role id must not be empty")
    private List<UUID> roleIds;
}
