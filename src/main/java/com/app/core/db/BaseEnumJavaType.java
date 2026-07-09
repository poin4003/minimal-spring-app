package com.app.core.db;

import java.sql.Types;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import com.app.core.base.BaseEnum;
import com.app.core.utils.EnumUtils;

public class BaseEnumJavaType<T extends Enum<T> & BaseEnum> extends AbstractClassJavaType<T> {

    public BaseEnumJavaType(Class<T> enumClass) {
        super(enumClass);
    }

    @Override
    public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
        return context.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor(Types.INTEGER);
    }

    @Override
    public String toString(T value) {
        return value != null ? String.valueOf(value.getCode()) : null;
    }

    @Override
    public T fromString(CharSequence string) {
        return string != null ? EnumUtils.fromCode(getJavaType(), Integer.parseInt(string.toString())) : null;
    }

    @Override
    public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
        if (value == null) return null;
        if (Integer.class.isAssignableFrom(type)) {
            return type.cast(value.getCode());
        }
        throw unknownUnwrap(type);
    }

    @Override
    public <X> T wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Integer intValue) { 
            return EnumUtils.fromCode(getJavaTypeClass(), intValue);
        }

        throw unknownWrap(value.getClass());
    }
}
