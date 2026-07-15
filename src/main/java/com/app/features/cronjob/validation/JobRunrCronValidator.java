package com.app.features.cronjob.validation;

import org.jobrunr.scheduling.cron.CronExpression;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class JobRunrCronValidator implements ConstraintValidator<ValidJobRunrCron, String> {

    private boolean allowBlank;

    @Override
    public void initialize(ValidJobRunrCron constraintAnnotation) {
        this.allowBlank = constraintAnnotation.allowBlank();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return allowBlank;
        }

        try {
            CronExpression.create(value.trim());
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
