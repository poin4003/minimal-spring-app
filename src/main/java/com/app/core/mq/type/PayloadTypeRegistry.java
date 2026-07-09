package com.app.core.mq.type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class PayloadTypeRegistry {

    private final Map<String, Class<?>> registry = new ConcurrentHashMap<>();

    public void register(String typeId, Class<?> clazz) {
        registry.put(typeId, clazz);
    }

    public Class<?> get(String typeId) {
        return registry.get(typeId);
    }
}
