package com.app.features.ui.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.app.config.settings.AppProperties;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class SocialHomePageController {

    private final AppProperties appProperties;

    @GetMapping("${app.ui.social-path:/}")
    public String home() {
        return "redirect:" + appProperties.getUi().getFeedPath();
    }
}
