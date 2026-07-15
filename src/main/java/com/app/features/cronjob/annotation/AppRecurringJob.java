package com.app.features.cronjob.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AppRecurringJob {

    String id();

    String name();

    String defaultCron();

    String zoneId() default "Asia/Ho_Chi_Minh";
}
