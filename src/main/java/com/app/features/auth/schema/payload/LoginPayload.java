package com.app.features.auth.schema.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginPayload {

    @NotBlank(message = "{validation.user.email.required}")
    @Email()
    private String email;

    @NotBlank(message = "{validation.user.password.required}")
    private String password;
}
