package com.app.features.email.service;

import com.app.features.email.enums.EmailTemplate;

import jakarta.validation.constraints.NotNull;

public interface EmailTemplateService {

    String render(
            @NotNull EmailTemplate template,
            @NotNull Object model);
}
