package com.app.features.ui.web.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.app.core.security.UserPrincipal;
import com.app.features.ui.web.support.UiShellFactory;
import com.app.features.ui.web.view.HomePageView;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomePageController {

    private final UiShellFactory uiShellFactory;

    @GetMapping("${app.ui.home-path:/admin}")
    public String home(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            Model model) {
        HomePageView page = HomePageView.builder()
                .shell(uiShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .build();

        model.addAttribute(HomePageView.ATTRIBUTE, page);
        return "home/index";
    }
}
