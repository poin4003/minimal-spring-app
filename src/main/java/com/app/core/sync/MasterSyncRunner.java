package com.app.core.sync;

import java.util.Comparator;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MasterSyncRunner implements ApplicationRunner {
    
    private final List<SyncableDataService> syncService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("================ [SYNC] MASTER DATA SYNC STARTED ================");

        syncService.sort(Comparator.comparingInt(SyncableDataService::getOrder));

        for (SyncableDataService service : syncService) {
            try {
                long start = System.currentTimeMillis();
                log.info(">> Syncing: [{}]", service.getSyncType());
                service.syncToDatabase();
                log.info("<< Done: [{}] ({}ms)", service.getSyncType(), System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.error("FAILED: [{}]", service.getSyncType(), e);
            }
        }

        log.info("======================= [SYNC] COMPLETE =========================");
    }
}
