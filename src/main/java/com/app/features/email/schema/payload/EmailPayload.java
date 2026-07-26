package com.app.features.email.schema.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmailPayload {

    @NotBlank
    @Email
    @Size(max = 255)
    private String recipientEmail;

    @NotBlank
    @Size(max = 500)
    private String subject;

    @NotBlank
    private String content;

    private boolean html;
}
