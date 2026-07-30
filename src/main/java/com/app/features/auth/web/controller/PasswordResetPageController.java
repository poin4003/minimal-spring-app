package com.app.features.auth.web.controller;

import org.springframework.http.HttpHeaders;
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
import com.app.core.exception.MyException;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.i18n.ExceptionMessageResolver;
import com.app.core.security.UserPrincipal;
import com.app.features.auth.enums.PasswordResetPageStep;
import com.app.features.auth.schema.payload.CompletePasswordResetPayload;
import com.app.features.auth.schema.payload.RequestPasswordResetOtpPayload;
import com.app.features.auth.schema.payload.VerifyPasswordResetOtpPayload;
import com.app.features.auth.schema.result.VerifyPasswordResetOtpResult;
import com.app.features.auth.service.PasswordResetService;
import com.app.features.auth.web.view.PasswordResetPageView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.forgot-password-path:/forgot-password}")
public class PasswordResetPageController {

    private static final String PAGE_VIEW = "auth/forgot-password";
    private static final String WORKSPACE_VIEW =
            "auth/forgot-password :: passwordResetWorkspace";

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final ExceptionMessageResolver exceptionMessageResolver;
    private final PasswordResetService passwordResetSvc;

    @ModelAttribute
    void preventPasswordResetCaching(
            HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    @GetMapping
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Model model) {
        if (currentUser != null) {
            return "redirect:" + appProperties.getUi().getHomePath();
        }

        return render(
                requestOtpPage(null),
                new RequestPasswordResetOtpPayload(),
                model,
                PAGE_VIEW);
    }

    @RateLimited(RateLimitPolicy.PASSWORD_RESET_OTP_IP)
    @PostMapping("/request-otp")
    public String requestOtp(
            @Valid @ModelAttribute(PasswordResetPageView.FORM_ATTRIBUTE)
            RequestPasswordResetOtpPayload form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model) {
        PasswordResetPageView page = requestOtpPage(null);

        if (bindingResult.hasErrors()) {
            return render(page, form, model, responseView(request));
        }

        try {
            passwordResetSvc.requestOtp(form);

            VerifyPasswordResetOtpPayload verifyForm =
                    new VerifyPasswordResetOtpPayload();
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

    @RateLimited(RateLimitPolicy.PASSWORD_RESET_VERIFY_IP)
    @PostMapping("/verify-otp")
    public String verifyOtp(
            @Valid @ModelAttribute(PasswordResetPageView.FORM_ATTRIBUTE)
            VerifyPasswordResetOtpPayload form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model) {
        PasswordResetPageView page = verifyOtpPage(null);

        if (bindingResult.hasErrors()) {
            return render(page, form, model, responseView(request));
        }

        try {
            VerifyPasswordResetOtpResult result =
                    passwordResetSvc.verifyOtp(form);

            CompletePasswordResetPayload completeForm =
                    new CompletePasswordResetPayload();
            completeForm.setResetToken(result.getResetToken());

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

    @RateLimited(RateLimitPolicy.PASSWORD_RESET_COMPLETE_IP)
    @PostMapping("/complete")
    public String complete(
            @Valid @ModelAttribute(PasswordResetPageView.FORM_ATTRIBUTE)
            CompletePasswordResetPayload form,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        PasswordResetPageView page = completePage(null);

        if (bindingResult.hasErrors()) {
            return render(page, form, model, responseView(request));
        }

        try {
            passwordResetSvc.completePasswordReset(form);
            return HtmxRequestSupport.redirectView(
                    request,
                    response,
                    appProperties.getUi().getLoginPath()
                            + "?passwordReset=true");
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
            PasswordResetPageView page,
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

    private PasswordResetPageView requestOtpPage(String formError) {
        return buildPage(
                PasswordResetPageStep.REQUEST_OTP,
                "auth.passwordReset.requestOtp.title",
                "auth.passwordReset.requestOtp.description",
                formError);
    }

    private PasswordResetPageView verifyOtpPage(String formError) {
        return buildPage(
                PasswordResetPageStep.VERIFY_OTP,
                "auth.passwordReset.verifyOtp.title",
                "auth.passwordReset.verifyOtp.description",
                formError);
    }

    private PasswordResetPageView completePage(String formError) {
        return buildPage(
                PasswordResetPageStep.COMPLETE,
                "auth.passwordReset.complete.title",
                "auth.passwordReset.complete.description",
                formError);
    }

    private PasswordResetPageView buildPage(
            PasswordResetPageStep step,
            String headingKey,
            String descriptionKey,
            String formError) {
        String passwordResetPath =
                appProperties.getUi().getForgotPasswordPath();
        String heading = messageResolver.get(headingKey);

        return PasswordResetPageView.builder()
                .step(step)
                .title(heading)
                .heading(heading)
                .description(messageResolver.get(descriptionKey))
                .applicationTitle(
                        appProperties.getUi().getApplicationTitle())
                .loginPath(appProperties.getUi().getLoginPath())
                .requestOtpPath(passwordResetPath + "/request-otp")
                .verifyOtpPath(passwordResetPath + "/verify-otp")
                .completePath(passwordResetPath + "/complete")
                .formError(formError)
                .build();
    }

    private String responseView(HttpServletRequest request) {
        return HtmxRequestSupport.isHtmxRequest(request)
                ? WORKSPACE_VIEW
                : PAGE_VIEW;
    }

    private String render(
            PasswordResetPageView page,
            Object form,
            Model model,
            String view) {
        String bindingResultAttribute =
                BindingResult.MODEL_KEY_PREFIX
                        + PasswordResetPageView.FORM_ATTRIBUTE;
        Object existingBindingResult =
                model.asMap().get(bindingResultAttribute);

        if (existingBindingResult instanceof BindingResult bindingResult
                && bindingResult.getTarget() != form) {
            model.asMap().remove(bindingResultAttribute);
        }

        model.addAttribute(PasswordResetPageView.ATTRIBUTE, page);
        model.addAttribute(PasswordResetPageView.FORM_ATTRIBUTE, form);
        return view;
    }
}
