package com.app.features.email.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.app.features.email.enums.EmailTemplate;
import com.app.features.email.service.EmailTemplateService;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class ThymeleafEmailTemplateService
        implements EmailTemplateService {

    private final SpringTemplateEngine templateEngine;

    @Override
    public String render(
            EmailTemplate template,
            Object model) {
        Context context = new Context();
        context.setVariable("email", model);

        return templateEngine.process(
                template.getPath(),
                context);
    }
}
