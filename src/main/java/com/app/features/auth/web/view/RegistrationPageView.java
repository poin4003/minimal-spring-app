package com.app.features.auth.web.view;

import com.app.features.auth.enums.RegistrationPageStep;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class RegistrationPageView {

    public static final String ATTRIBUTE = "page";
    public static final String FORM_ATTRIBUTE = "form";

    private final RegistrationPageStep step;
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
        return step == RegistrationPageStep.REQUEST_OTP;
    }

    public boolean isVerifyOtpStep() {
        return step == RegistrationPageStep.VERIFY_OTP;
    }

    public boolean isCompleteStep() {
        return step == RegistrationPageStep.COMPLETE;
    }
}
