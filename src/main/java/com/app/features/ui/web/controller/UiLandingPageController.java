package com.app.features.ui.web.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.app.core.security.UserPrincipal;
import com.app.features.ui.web.support.UiLandingPathResolver;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UiLandingPageController {

    private final UiLandingPathResolver landingPathResolver;

    @GetMapping("${app.ui.landing-path:/home}")
    public String landing(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return "redirect:" + landingPathResolver.resolve(currentUser);
    }
}
