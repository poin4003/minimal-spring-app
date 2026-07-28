package com.app.features.auth.schema.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenPayload {

    @NotBlank(message = "{validation.auth.refreshToken.required}")
    private String refreshToken;
}
