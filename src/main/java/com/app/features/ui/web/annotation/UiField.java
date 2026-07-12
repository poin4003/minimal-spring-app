package com.app.features.ui.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.app.features.ui.web.enums.UiInputType;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UiField {

    String key() default "";

    String label();

    int order() default Integer.MAX_VALUE;

    UiInputType type() default UiInputType.TEXT;

    String placeholder() default "";

    String helpText() default "";

    boolean required() default false;

    boolean readOnly() default false;

    int rows() default 3;
}
