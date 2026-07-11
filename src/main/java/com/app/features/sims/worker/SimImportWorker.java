package com.app.features.sims.worker;

import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

import com.app.features.sims.schema.payload.CreateSimPayload;
import com.app.features.sims.service.SimService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimImportWorker {

    private SimService simSvc;

    @Job(name = "Import SIM - phone: %0", retries = 3)
    public void execute(CreateSimPayload payload) {
        if (payload == null) {
            log.error("Received null payload. Skipping");
            return;
        }

        log.info("Processing created Sim: {}", payload);

        try {
            simSvc.createSim(payload);
        } catch (Exception e) {
            log.error("Error processing SIM import for phone number {}: {}", payload.getPhoneNumber(), e.getMessage());
            throw e;
        }
    }
}
