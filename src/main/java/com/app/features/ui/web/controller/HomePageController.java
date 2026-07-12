package com.app.features.ui.web.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.app.config.settings.AppProperties;
import com.app.core.menu.MenuService;
import com.app.core.security.UserPrincipal;
import com.app.features.ui.web.view.HomePageView;
import com.app.features.ui.web.view.UiCurrentUserView;
import com.app.features.ui.web.view.UiShellView;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomePageController {

    private final AppProperties appProperties;
    private final MenuService menuService;

    @GetMapping("${app.ui.home-path:/admin}")
    public String home(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            Model model) {
        HomePageView page = HomePageView.builder()
                .shell(UiShellView.builder()
                        .title(appProperties.getUi().getApplicationTitle())
                        .logoutPath(appProperties.getUi().getLogoutPath())
                        .currentUser(UiCurrentUserView.builder()
                                .email(currentUser.getEmail())
                                .authorities(currentUser.getAuthorities().stream()
                                        .map(authority -> authority.getAuthority())
                                        .toList())
                                .build())
                        .menuTree(menuService.getMenuTree(request.getRequestURI()))
                        .build())
                .build();

        model.addAttribute(HomePageView.ATTRIBUTE, page);
        return "home/index";
    }
}
