package com.app.config.kafka.dlq.impl;

import java.util.Objects;

import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.messaging.Message;
import org.springframework.lang.NonNull;

import com.app.config.kafka.KafkaHeaderMapper;
import com.app.config.kafka.dlq.DlqTopicResolver;
import com.app.core.mq.dlq.DlqMetadata;
import com.app.core.mq.dlq.DlqPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaDlqPublisher implements DlqPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DlqTopicResolver dlqTopicResolver;

    @Override
    public void publish(
        @NonNull Object payload, 
        @NonNull Headers headers, 
        @NonNull DlqMetadata metadata
    ) {
        Objects.requireNonNull(payload, "DLQ payload must not be null");
        Objects.requireNonNull(headers, "Kafka headers must not be null");
        Objects.requireNonNull(metadata, "DlqMetadata must not be null");

        String dlqTopic = Objects.requireNonNull(dlqTopicResolver.resolve(
                metadata.getOriginalTopic(), 
                metadata.getConsumerGroup()
            ),
            "DLQ topic must be not null"
        );

        Message<Object> message = MessageBuilder
            .withPayload(payload)
            .copyHeaders(KafkaHeaderMapper.toMap(headers))
            .setHeader("x-dlq-reason", metadata.getException().getMessage())
            .setHeader("x-original-topic", metadata.getOriginalTopic())
            .setHeader("x-consumer-group", metadata.getConsumerGroup())
            .build();

        kafkaTemplate.send(dlqTopic, message);
    }
}
