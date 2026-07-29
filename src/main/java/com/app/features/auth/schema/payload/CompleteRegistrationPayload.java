package com.app.features.auth.schema.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompleteRegistrationPayload {

    @NotBlank(message = "{validation.registration.completionToken.required}")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]{43}$",
            message = "{validation.registration.completionToken.format}")
    private String completionToken;

    @NotBlank(message = "{validation.user.password.required}")
    @Size(
            min = 8,
            max = 72,
            message = "{validation.user.password.size}")
    private String password;
}
