package com.app.config.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.common.header.Headers;

public final class KafkaHeaderMapper {
    
    private KafkaHeaderMapper() {}

    public static Map<String, Object> toMap(Headers headers) {
        Map<String, Object> map = new HashMap<>();
        headers.forEach(h -> map.put(h.key(), h.value()));
        return map;
    }
}
