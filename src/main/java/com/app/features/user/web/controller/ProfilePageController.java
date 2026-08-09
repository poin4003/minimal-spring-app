package com.app.features.user.web.controller;

import org.modelmapper.ModelMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.config.security.web.HtmxRequestSupport;
import com.app.config.settings.AppProperties;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.security.UserPrincipal;
import com.app.features.ui.web.support.SocialShellFactory;
import com.app.features.user.schema.payload.UpdateProfilePayload;
import com.app.features.user.schema.result.ProfileResult;
import com.app.features.user.service.ProfileService;
import com.app.features.user.web.support.ProfilePreferenceCookieService;
import com.app.features.user.web.view.ProfileForm;
import com.app.features.user.web.view.ProfilePageView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.profile-path:/profile}")
public class ProfilePageController {

    private static final String EMPTY_HTMX_VIEW =
            "fragments/components/htmx-response :: empty";

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final SocialShellFactory socialShellFactory;
    private final ProfileService profileSvc;
    private final ProfilePreferenceCookieService profilePreferenceCookieSvc;
    private final ModelMapper mapper;

    @GetMapping
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            Model model) {
        ProfileResult profile = profileSvc.getProfile(currentUser.getUserId());
        ProfileForm form = mapper.map(profile, ProfileForm.class);

        model.addAttribute(
                ProfilePageView.ATTRIBUTE,
                buildPage(currentUser, request, profile));
        model.addAttribute(ProfileForm.ATTRIBUTE, form);
        return "profile/index";
    }

    @PostMapping
    public String update(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @ModelAttribute(ProfileForm.ATTRIBUTE) ProfileForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    ProfilePageView.ATTRIBUTE,
                    buildPage(
                            currentUser,
                            request,
                            profileSvc.getProfile(currentUser.getUserId())));
            return "profile/index";
        }

        ProfileResult profile = profileSvc.updateProfile(
                currentUser.getUserId(),
                mapper.map(form, UpdateProfilePayload.class));
        profilePreferenceCookieSvc.writePreferences(
                request,
                response,
                profile);

        return HtmxRequestSupport.redirectView(
                request,
                response,
                getProfilePath());
    }

    @PostMapping("/theme")
    public String updateTheme(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam boolean darkThemeEnabled) {
        ProfileResult profile = profileSvc.updateTheme(
                currentUser.getUserId(),
                darkThemeEnabled);
        profilePreferenceCookieSvc.writeTheme(
                response,
                profile.isDarkThemeEnabled());

        if (HtmxRequestSupport.isHtmxRequest(request)) {
            return EMPTY_HTMX_VIEW;
        }
        return "redirect:" + getProfilePath();
    }

    @PostMapping("/avatar/remove")
    public String removeAvatar(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response) {
        profileSvc.removeAvatar(currentUser.getUserId());
        return HtmxRequestSupport.redirectView(
                request,
                response,
                getProfilePath());
    }

    private ProfilePageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            ProfileResult profile) {
        return ProfilePageView.builder()
                .title(messageResolver.get("profile.title"))
                .updatePath(getProfilePath())
                .avatarSelectionPath(getProfilePath() + "/avatar")
                .removeAvatarPath(getProfilePath() + "/avatar/remove")
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .profile(profile)
                .build();
    }

    private String getProfilePath() {
        return appProperties.getUi().getProfilePath();
    }
}
