package com.app.core.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.app.core.base.BaseEnum;

public final class EnumUtils {
    private static final Map<Class<? extends BaseEnum>, Object[]> enumValueCache =
        new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private static <T extends Enum<T> & BaseEnum> T[] getValues(Class<T> enumClass) {
        return (T[]) enumValueCache.computeIfAbsent(enumClass, Class::getEnumConstants);
    }

    /**
     * Generic to find Enum constant from code.
     *
     * @param enumClass Enum class
     * @param code      value int (code)
     * @return
     * @throws IllegalArgumentException
     */
    public static <T extends Enum<T> & BaseEnum> T fromCode(Class<T> enumClass, int code) {
        for (T constant : getValues(enumClass)) {
            if (constant.getCode() == code) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + code + " for enum " + enumClass.getSimpleName());
    }
}
