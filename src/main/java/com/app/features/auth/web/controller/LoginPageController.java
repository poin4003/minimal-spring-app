package com.app.features.auth.web.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.config.settings.AppProperties;
import com.app.core.security.UserPrincipal;
import com.app.core.utils.HttpUtils;
import com.app.features.auth.schema.payload.LoginPayload;
import com.app.features.auth.service.AuthService;
import com.app.features.auth.web.support.AuthCookieService;
import com.app.features.auth.web.view.LoginPageView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class LoginPageController {

    private final AppProperties appProperties;
    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @GetMapping("${app.ui.login-path:/login}")
    public String loginPage(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout,
            Model model) {

        if (currentUser != null) {
            return "redirect:" + appProperties.getUi().getHomePath();
        }

        LoginPageView page = LoginPageView.builder()
                .title(appProperties.getUi().getLoginTitle())
                .applicationTitle(appProperties.getUi().getApplicationTitle())
                .loginPath(appProperties.getUi().getLoginPath())
                .hasError(error != null)
                .loggedOut(logout != null)
                .build();

        model.addAttribute(LoginPageView.ATTRIBUTE, page);

        return "auth/login";
    }

    @PostMapping("${app.ui.login-path:/login}")
    public String login(
            @ModelAttribute LoginPayload form,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            authCookieService.writeAuthenticationCookie(
                    response,
                    authService.login(form, HttpUtils.getClientIp(request)).getAccessToken());

            return "redirect:" + appProperties.getUi().getHomePath();
        } catch (RuntimeException ex) {
            authCookieService.clearAuthenticationCookies(response);
            return "redirect:" + appProperties.getUi().getLoginPath() + "?error";
        }
    }

    @PostMapping("${app.ui.logout-path:/logout}")
    public String logout(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletResponse response) {
        if (currentUser != null && currentUser.getKeyStore() != null) {
            authService.logout(currentUser.getUserId(), currentUser.getKeyStore().getId());
        }

        SecurityContextHolder.clearContext();
        authCookieService.clearAuthenticationCookies(response);
        return "redirect:" + appProperties.getUi().getLoginPath() + "?logout";
    }
}
