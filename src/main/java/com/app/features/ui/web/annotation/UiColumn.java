package com.app.features.ui.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.app.features.ui.web.enums.UiCellType;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UiColumn {

    String key() default "";

    String label();

    int order() default Integer.MAX_VALUE;

    UiCellType type() default UiCellType.TEXT;

    String emptyValue() default "-";

    String badgeClass() default "";
}
