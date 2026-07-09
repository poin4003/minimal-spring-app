package com.app.features.cronjob.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CronJobDef {
    
    String jobName();

    String cronExpression() default "0 0 0 * * ?";

    String lockAtMostFor() default "PT10M";
    String lockAtLeastFor() default "PT30S";

    String description() default "Auto-synced Job";
}
