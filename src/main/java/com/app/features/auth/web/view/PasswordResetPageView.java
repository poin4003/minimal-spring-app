package com.app.features.auth.web.view;

import com.app.features.auth.enums.PasswordResetPageStep;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class PasswordResetPageView {

    public static final String ATTRIBUTE = "page";
    public static final String FORM_ATTRIBUTE = "form";

    private final PasswordResetPageStep step;
    private final String title;
    private final String heading;
    private final String description;
    private final String applicationTitle;
    private final String loginPath;
    private final String requestOtpPath;
    private final String verifyOtpPath;
    private final String completePath;
    private final String formError;

    public boolean isRequestOtpStep() {
        return step == PasswordResetPageStep.REQUEST_OTP;
    }

    public boolean isVerifyOtpStep() {
        return step == PasswordResetPageStep.VERIFY_OTP;
    }

    public boolean isCompleteStep() {
        return step == PasswordResetPageStep.COMPLETE;
    }
}
