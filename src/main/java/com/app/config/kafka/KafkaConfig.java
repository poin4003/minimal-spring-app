package com.app.config.kafka;

import java.util.Objects;

import org.apache.kafka.common.errors.SerializationException;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import com.app.core.mq.dlq.DlqMetadata;
import com.app.core.mq.dlq.DlqPublisher;
import com.app.core.mq.type.LogicalTypeIdMapper;
import com.app.core.mq.type.PayloadTypeRegistry;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import io.jsonwebtoken.io.DeserializationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;
    private final PayloadTypeRegistry payloadTypeRegistry; 

    @Bean
    public ConsumerFactory<Object, Object> consumerFactory() {
        var props = Objects.requireNonNull(kafkaProperties.buildConsumerProperties(null), "Kafka properties can not be null");
        var factory = new DefaultKafkaConsumerFactory<Object, Object>(props);

        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>();
        jsonDeserializer.setTypeMapper(new LogicalTypeIdMapper(payloadTypeRegistry));
        jsonDeserializer.setRemoveTypeHeaders(false);
        jsonDeserializer.setUseTypeMapperForKey(false);
        jsonDeserializer.addTrustedPackages("*"); 

        ErrorHandlingDeserializer<Object> errorHandlingDeserializer = new ErrorHandlingDeserializer<>(jsonDeserializer);
        factory.setValueDeserializer(errorHandlingDeserializer);

        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory,
            DlqPublisher dlqPublisher) {

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(Objects.requireNonNull(consumerFactory, "consumer factory can not be null"));

        var recoverer = new org.springframework.kafka.listener.ConsumerRecordRecoverer() {
            @Override
            public void accept(org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record, Exception ex) {
                if (record == null || record.value() == null) return;

                log.error("Message complete failure after retry. Send to DLQ... Topic: {}", record.topic());

                dlqPublisher.publish(
                        Objects.requireNonNull(record.value(), "Payload must not be null"),
                        Objects.requireNonNull(record.headers(), "Dlq headers must not be null"),
                        Objects.requireNonNull(
                                DlqMetadata.builder()
                                        .originalTopic(Objects.requireNonNull(record.topic(), "topic"))
                                        .consumerGroup("global-group")
                                        .exception(Objects.requireNonNull(ex, "exception"))
                                        .build(),
                                "DlqMetadata must not be null"));
            }
        };

        FixedBackOff backOff = new FixedBackOff(1000L, 3);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        errorHandler.addNotRetryableExceptions(
                SerializationException.class,
                DeserializationException.class,
                InvalidFormatException.class);

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
