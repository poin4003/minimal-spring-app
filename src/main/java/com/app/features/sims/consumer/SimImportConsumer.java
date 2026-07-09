package com.app.features.sims.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.app.core.exception.ExceptionFactory;
import com.app.features.sims.cqrs.command.CreateSimCmd;

import an.awesome.pipelinr.Pipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimImportConsumer {

    private final Pipeline pipeline;

    @KafkaListener(
        id = "simImport",
        topics = "import-sim-topic", 
        groupId = "sim-import-group",
        concurrency = "${app.mq.consumers.sim-import.concurrency:1}"
    )
    public void handle(CreateSimCmd cmd) {
        if (cmd == null) {
            log.error("Received null request due to deserialization error. Skipping");
            return;
        }

        if ("9999999999".equals(cmd.getPhoneNumber())) {
            log.warn(">>> SIMULATION: Triggering Artificial Error for DLQ Test! <<<");
            throw ExceptionFactory.importSimError("Simulator import sim error");
        }

        log.info("Received created Sim: {}", cmd);

        try {
            pipeline.send(cmd);
        } catch (Exception e) {
            log.error("Error processing SIM import for phone number {}: {}", cmd.getPhoneNumber(), e.getMessage());
            throw e;
        }
    }
}
