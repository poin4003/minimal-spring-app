package com.app.features.user.web.view;

import com.app.features.ui.web.annotation.UiField;
import com.app.features.ui.web.enums.UiInputType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateUserModalForm {

    @UiField(
            label = "Email",
            order = 10,
            type = UiInputType.EMAIL,
            placeholder = "user@example.com",
            required = true)
    @NotBlank(message = "{validation.user.email.required}")
    @Email(message = "{validation.user.email.invalid}")
    private String email;

    @UiField(
            label = "Password",
            order = 20,
            type = UiInputType.PASSWORD,
            placeholder = "At least 6 characters",
            helpText = "The password can be changed later.",
            required = true)
    @NotBlank(message = "{validation.user.password.required}")
    @Size(min = 6, message = "{validation.user.password.min}")
    private String password;
}
