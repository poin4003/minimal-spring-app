package com.app.features.ai.vision.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import com.app.config.settings.AppProperties;
import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.vision.service.AiVisionHealthClient;

@ExtendWith(MockitoExtension.class)
class AiVisionCapabilityTests {

    @Mock
    private ObjectProvider<AiVisionHealthClient> healthClientProvider;

    @Mock
    private AiVisionHealthClient healthClient;

    private AppProperties appProperties;
    private AiVisionCapability capability;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        capability = new AiVisionCapability(
                appProperties,
                healthClientProvider);
    }

    @Test
    void reportsDisabledWithoutResolvingRuntime() {
        appProperties.getAi().getVision().setEnabled(false);

        assertThat(capability.resolveAvailability())
                .isEqualTo(AiAvailability.DISABLED);
        verifyNoInteractions(healthClientProvider);
    }

    @Test
    void reportsUnavailableWhenEnabledRuntimeIsMissing() {
        appProperties.getAi().getVision().setEnabled(true);
        given(healthClientProvider.getIfAvailable()).willReturn(null);

        assertThat(capability.resolveAvailability())
                .isEqualTo(AiAvailability.UNAVAILABLE);
    }

    @Test
    void reportsUnavailableWhenRuntimeIsNotReady() {
        appProperties.getAi().getVision().setEnabled(true);
        given(healthClientProvider.getIfAvailable()).willReturn(healthClient);
        given(healthClient.isReady()).willReturn(false);

        assertThat(capability.resolveAvailability())
                .isEqualTo(AiAvailability.UNAVAILABLE);
    }

    @Test
    void reportsReadyWhenRuntimeIsReady() {
        appProperties.getAi().getVision().setEnabled(true);
        given(healthClientProvider.getIfAvailable()).willReturn(healthClient);
        given(healthClient.isReady()).willReturn(true);

        assertThat(capability.resolveAvailability())
                .isEqualTo(AiAvailability.READY);
    }
}
