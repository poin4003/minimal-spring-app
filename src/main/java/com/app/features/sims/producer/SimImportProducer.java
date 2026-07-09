package com.app.features.sims.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.mapping.AbstractJavaTypeMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.app.features.sims.cqrs.command.CreateSimCmd;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimImportProducer {

    private static final String TOPIC_NAME = "import-sim-topic";
    private final KafkaTemplate<String, CreateSimCmd> kafkaTemplate;

    public void sendSimToImportQueue(CreateSimCmd cmd) {
        if (cmd == null || cmd.getPhoneNumber() == null) {
            log.error("Cannot send message: Command or Key (phoneNumber) is null");
            return;
        }

        log.info("Sending SIM with phone number {} to kafka topic {}",
                cmd.getPhoneNumber(), TOPIC_NAME);

        String key = cmd.getPhoneNumber();

        Message<CreateSimCmd> message = MessageBuilder
                .withPayload(cmd)
                .setHeader(KafkaHeaders.TOPIC, TOPIC_NAME)
                .setHeader(KafkaHeaders.KEY, key)
                .setHeader(
                        AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
                        CreateSimCmd.class.getSimpleName())
                .build();

        kafkaTemplate.send(message)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully send SIM {} to Partition {} with Offset {}",
                                key,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send SIM {}: {}", key, ex.getMessage());
                    }

                });
    }
}
