package com.app.features.cronjob.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = JobRunrCronValidator.class)
public @interface ValidJobRunrCron {

    String message() default "Cron expression is invalid for JobRunr.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    boolean allowBlank() default true;
}
