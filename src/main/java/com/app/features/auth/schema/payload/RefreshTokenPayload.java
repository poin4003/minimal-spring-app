package com.app.features.auth.schema.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenPayload {

    @NotBlank(message = "Refresh token must be not blank")
    private String refreshToken;
}
