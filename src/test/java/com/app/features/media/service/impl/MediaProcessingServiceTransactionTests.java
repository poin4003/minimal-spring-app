package com.app.features.media.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.app.features.media.repository.MediaRepository;
import com.app.features.media.service.MediaProcessingLeaseService;
import com.app.features.media.service.MediaProcessingService;

@SpringBootTest
class MediaProcessingServiceTransactionTests {

    @Autowired
    private MediaProcessingService mediaProcessingSvc;

    @MockitoBean
    private MediaRepository mediaRepo;

    @MockitoBean
    private MediaProcessingLeaseService mediaProcessingLeaseSvc;

    @Test
    void privatePreparationRunsInsideTransaction() {
        AtomicBoolean transactionActive = new AtomicBoolean(false);
        given(mediaProcessingLeaseSvc.acquire(any(UUID.class), any(UUID.class)))
                .willReturn(true);
        given(mediaRepo.findById(any(UUID.class))).willAnswer(invocation -> {
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            return Optional.empty();
        });

        mediaProcessingSvc.process(UUID.randomUUID(), UUID.randomUUID());

        assertThat(transactionActive).isTrue();
    }
}
