package com.app.features.user.schema.payload;

import com.app.core.enums.AppLanguage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfilePayload {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must not exceed 150 characters")
    private String fullName;

    @NotNull(message = "Language is required")
    private AppLanguage language;

    private boolean darkThemeEnabled;
}
