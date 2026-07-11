package com.app.features.auth.schema.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginPayload {

    @NotBlank(message = "Email must be not blank")
    @Email()
    private String email;

    @NotBlank(message = "Password must be not blank")
    private String password;
}
