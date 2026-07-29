package com.app.features.auth.schema.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyRegistrationOtpPayload {

    @NotBlank(message = "{validation.user.email.required}")
    @Email(message = "{validation.user.email.invalid}")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "{validation.registration.otp.required}")
    @Pattern(
            regexp = "^\\d{6,8}$",
            message = "{validation.registration.otp.format}")
    private String code;
}
