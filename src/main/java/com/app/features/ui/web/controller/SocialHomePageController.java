package com.app.features.ui.web.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.app.core.security.UserPrincipal;
import com.app.features.ui.web.support.SocialShellFactory;
import com.app.features.ui.web.view.SocialHomePageView;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class SocialHomePageController {

    private final SocialShellFactory socialShellFactory;

    @GetMapping("${app.ui.social-path:/}")
    public String home(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            Model model) {
        SocialHomePageView page = SocialHomePageView.builder()
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .build();

        model.addAttribute(SocialHomePageView.ATTRIBUTE, page);
        return "social/home";
    }
}
