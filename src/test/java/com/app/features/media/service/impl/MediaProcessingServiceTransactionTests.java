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
import com.app.features.media.service.MediaProcessingService;

@SpringBootTest
class MediaProcessingServiceTransactionTests {

    @Autowired
    private MediaProcessingService mediaProcessingService;

    @MockitoBean
    private MediaRepository mediaRepository;

    @Test
    void privatePreparationRunsInsideTransaction() {
        AtomicBoolean transactionActive = new AtomicBoolean(false);
        given(mediaRepository.findById(any(UUID.class))).willAnswer(invocation -> {
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            return Optional.empty();
        });

        mediaProcessingService.process(UUID.randomUUID());

        assertThat(transactionActive).isTrue();
    }
}
