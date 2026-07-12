package com.app.features.ui.web.component.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

import org.springframework.util.ReflectionUtils;

final class UiComponentFieldSupport {

    private UiComponentFieldSupport() {
    }

    static <A extends Annotation> List<Field> findAnnotatedFields(
            Class<?> type,
            Class<A> annotationType,
            ToIntFunction<A> orderExtractor) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;

        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                A annotation = field.getAnnotation(annotationType);
                if (annotation == null) {
                    continue;
                }

                ReflectionUtils.makeAccessible(field);
                fields.add(field);
            }

            current = current.getSuperclass();
        }

        fields.sort(Comparator
                .comparingInt((Field field) -> orderExtractor.applyAsInt(field.getAnnotation(annotationType)))
                .thenComparing(field -> field.getName()));

        return fields;
    }

    static Object readValue(Field field, Object target) {
        if (target == null) {
            return null;
        }

        return ReflectionUtils.getField(field, target);
    }
}
