package com.app.features.auth.service;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public interface AccountCredentialService {

    void updatePassword(
            @NotNull UUID userId,
            @NotBlank(message = "{validation.user.password.required}")
            @Size(
                    min = 8,
                    max = 72,
                    message = "{validation.user.password.size}")
            String password);
}
