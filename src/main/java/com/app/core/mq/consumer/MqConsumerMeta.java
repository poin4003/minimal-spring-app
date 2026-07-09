package com.app.core.mq.consumer;

import org.springframework.lang.NonNull;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MqConsumerMeta {
    
    @NonNull
    private String listenerId;

    @NonNull
    private String topic;

    @NonNull
    private String consumerGroup;

    private boolean dlq;

    @NonNull
    private Class<?> consumerClass;
}
