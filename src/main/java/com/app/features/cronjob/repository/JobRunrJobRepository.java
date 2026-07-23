package com.app.features.cronjob.repository;

import java.time.Instant;

import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProvider;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JobRunrJobRepository {

    private final StorageProvider storageProvider;

    public int deleteFailedJobsUpdatedBefore(Instant cutoff) {
        return storageProvider.deleteJobsPermanently(StateName.FAILED, cutoff);
    }
}
