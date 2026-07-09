package com.app.core.mq.consumer;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class MqConsumerRegistry {
    
    private final Map<String, MqConsumerMeta> consumers = new ConcurrentHashMap<>();

    public void register(@NonNull MqConsumerMeta meta) {
        consumers.put(meta.getListenerId(), meta);
    }

    public Collection<MqConsumerMeta> getAll() {
        return consumers.values();
    }

    public Optional<MqConsumerMeta> get(String listenerId) {
        return Optional.ofNullable(consumers.get(listenerId));
    }
}
