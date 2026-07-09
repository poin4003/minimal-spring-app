package com.app.core.mq.type;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper;
import org.springframework.kafka.support.mapping.AbstractJavaTypeMapper;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class LogicalTypeIdMapper extends DefaultJackson2JavaTypeMapper {

    private final PayloadTypeRegistry registry;

    public LogicalTypeIdMapper(PayloadTypeRegistry registry) {
        this.registry = registry;
        setTypePrecedence(TypePrecedence.TYPE_ID);
    }

    @Override
    public JavaType toJavaType(Headers headers) {

        if (headers == null) {
            throw new IllegalStateException("Kafka headers are null");
        }

        var typeHeader = headers.lastHeader(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME);

        if (typeHeader == null) {
            throw new IllegalStateException("__TypeId__ header missing");
        }

        String logicalType = new String(typeHeader.value(), StandardCharsets.UTF_8);

        Class<?> clazz = registry.get(logicalType);

        if (clazz == null) {
            throw new IllegalStateException(
                "No payload mapping found for logicalType: " + logicalType
            );
        }

        return TypeFactory.defaultInstance().constructType(clazz);
    }
}
