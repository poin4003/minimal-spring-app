package com.app.features.auth.web.controller;

import java.util.Locale;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.app.config.ratelimit.RateLimitPolicy;
import com.app.config.ratelimit.RateLimited;
import com.app.config.security.web.HtmxRequestSupport;
import com.app.config.settings.AppProperties;
import com.app.core.enums.AppLanguage;
import com.app.core.exception.MyException;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.i18n.ExceptionMessageResolver;
import com.app.core.security.UserPrincipal;
import com.app.core.utils.HttpUtils;
import com.app.features.auth.enums.RegistrationPageStep;
import com.app.features.auth.schema.payload.CompleteRegistrationPayload;
import com.app.features.auth.schema.payload.RequestRegistrationOtpPayload;
import com.app.features.auth.schema.payload.VerifyRegistrationOtpPayload;
import com.app.features.auth.schema.result.LoginResult;
import com.app.features.auth.schema.result.VerifyRegistrationOtpResult;
import com.app.features.auth.service.RegistrationService;
import com.app.features.auth.web.support.AuthCookieService;
import com.app.features.auth.web.view.RegistrationPageView;
import com.app.features.user.schema.result.ProfileResult;
import com.app.features.user.service.ProfileService;
import com.app.features.user.web.support.ProfilePreferenceCookieService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.registration-path:/register}")
public class RegistrationPageController {

    private static final String PAGE_VIEW = "auth/register";
    private static final String WORKSPACE_VIEW =
            "auth/register :: registrationWorkspace";

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final ExceptionMessageResolver exceptionMessageResolver;
    private final RegistrationService registrationSvc;
    private final AuthCookieService authCookieSvc;
    private final ProfileService profileSvc;
    private final ProfilePreferenceCookieService profilePreferenceCookieSvc;

    @GetMapping
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Model model) {
        if (currentUser != null) {
            return "redirect:" + appProperties.getUi().getHomePath();
        }

        return render(
                requestOtpPage(null),
                new RequestRegistrationOtpPayload(),
                model,
                PAGE_VIEW);
    }

    @RateLimited(RateLimitPolicy.REGISTRATION_OTP_IP)
    @PostMapping("/request-otp")
    public String requestOtp(
            @Valid @ModelAttribute(RegistrationPageView.FORM_ATTRIBUTE)
            RequestRegistrationOtpPayload form,
            BindingResult bindingResult,
            Locale locale,
            HttpServletRequest request,
            Model model) {
        RegistrationPageView page = requestOtpPage(null);

        if (bindingResult.hasErrors()) {
            return render(page, form, model, responseView(request));
        }

        try {
            registrationSvc.requestOtp(
                    form,
                    AppLanguage.fromLocale(locale).orElse(AppLanguage.EN));

            VerifyRegistrationOtpPayload verifyForm =
                    new VerifyRegistrationOtpPayload();
            verifyForm.setEmail(form.getEmail());

            return render(
                    verifyOtpPage(null),
                    verifyForm,
                    model,
                    responseView(request));
        } catch (MyException exception) {
            return renderExpectedError(
                    exception,
                    page,
                    form,
                    request,
                    model);
        }
    }

    @RateLimited(RateLimitPolicy.REGISTRATION_VERIFY_IP)
    @PostMapping("/verify-otp")
    public String verifyOtp(
            @Valid @ModelAttribute(RegistrationPageView.FORM_ATTRIBUTE)
            VerifyRegistrationOtpPayload form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model) {
        RegistrationPageView page = verifyOtpPage(null);

        if (bindingResult.hasErrors()) {
            return render(page, form, model, responseView(request));
        }

        try {
            VerifyRegistrationOtpResult result =
                    registrationSvc.verifyOtp(form);

            CompleteRegistrationPayload completeForm =
                    new CompleteRegistrationPayload();
            completeForm.setCompletionToken(result.getCompletionToken());

            return render(
                    completePage(null),
                    completeForm,
                    model,
                    responseView(request));
        } catch (MyException exception) {
            return renderExpectedError(
                    exception,
                    page,
                    form,
                    request,
                    model);
        }
    }

    @RateLimited(RateLimitPolicy.REGISTRATION_COMPLETE_IP)
    @PostMapping("/complete")
    public String complete(
            @Valid @ModelAttribute(RegistrationPageView.FORM_ATTRIBUTE)
            CompleteRegistrationPayload form,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        RegistrationPageView page = completePage(null);

        if (bindingResult.hasErrors()) {
            return render(page, form, model, responseView(request));
        }

        try {
            LoginResult tokens = registrationSvc.completeRegistration(
                    form,
                    HttpUtils.getClientIp(request));
            ProfileResult profile =
                    profileSvc.getProfile(tokens.getUserId());

            authCookieSvc.writeAuthenticationCookies(response, tokens);
            profilePreferenceCookieSvc.writePreferences(
                    request,
                    response,
                    profile);

            return HtmxRequestSupport.redirectView(
                    request,
                    response,
                    appProperties.getUi().getHomePath());
        } catch (MyException exception) {
            form.setPassword(null);
            return renderExpectedError(
                    exception,
                    page,
                    form,
                    request,
                    model);
        }
    }

    private String renderExpectedError(
            MyException exception,
            RegistrationPageView page,
            Object form,
            HttpServletRequest request,
            Model model) {
        if (exception.getHttpStatusCode() >= 500) {
            throw exception;
        }

        return render(
                page.toBuilder()
                        .formError(exceptionMessageResolver.resolve(exception))
                        .build(),
                form,
                model,
                responseView(request));
    }

    private RegistrationPageView requestOtpPage(String formError) {
        return buildPage(
                RegistrationPageStep.REQUEST_OTP,
                "auth.registration.requestOtp.title",
                "auth.registration.requestOtp.description",
                formError);
    }

    private RegistrationPageView verifyOtpPage(String formError) {
        return buildPage(
                RegistrationPageStep.VERIFY_OTP,
                "auth.registration.verifyOtp.title",
                "auth.registration.verifyOtp.description",
                formError);
    }

    private RegistrationPageView completePage(String formError) {
        return buildPage(
                RegistrationPageStep.COMPLETE,
                "auth.registration.complete.title",
                "auth.registration.complete.description",
                formError);
    }

    private RegistrationPageView buildPage(
            RegistrationPageStep step,
            String headingKey,
            String descriptionKey,
            String formError) {
        String registrationPath =
                appProperties.getUi().getRegistrationPath();
        String heading = messageResolver.get(headingKey);

        return RegistrationPageView.builder()
                .step(step)
                .title(heading)
                .heading(heading)
                .description(messageResolver.get(descriptionKey))
                .applicationTitle(
                        appProperties.getUi().getApplicationTitle())
                .loginPath(appProperties.getUi().getLoginPath())
                .requestOtpPath(registrationPath + "/request-otp")
                .verifyOtpPath(registrationPath + "/verify-otp")
                .completePath(registrationPath + "/complete")
                .formError(formError)
                .build();
    }

    private String responseView(HttpServletRequest request) {
        return HtmxRequestSupport.isHtmxRequest(request)
                ? WORKSPACE_VIEW
                : PAGE_VIEW;
    }

    private String render(
            RegistrationPageView page,
            Object form,
            Model model,
            String view) {
        String bindingResultAttribute =
                BindingResult.MODEL_KEY_PREFIX
                        + RegistrationPageView.FORM_ATTRIBUTE;
        Object existingBindingResult =
                model.asMap().get(bindingResultAttribute);

        if (existingBindingResult instanceof BindingResult bindingResult
                && bindingResult.getTarget() != form) {
            // Do not bind the previous step's validation state to a new payload.
            model.asMap().remove(bindingResultAttribute);
        }

        model.addAttribute(RegistrationPageView.ATTRIBUTE, page);
        model.addAttribute(RegistrationPageView.FORM_ATTRIBUTE, form);
        return view;
    }
}
