package com.app.core.mq.dlq;

import org.apache.kafka.common.header.Headers;
import org.springframework.lang.NonNull;

public interface DlqPublisher {

    void publish(
        @NonNull Object payload,
        @NonNull Headers headers,
        @NonNull DlqMetadata metadata
    );
}
