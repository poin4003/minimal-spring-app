package com.app.core.mq.dlq;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DlqMetadata {

    @NonNull
    private String originalTopic; 

    @NonNull
    private String consumerGroup;

    @Nullable
    private String listenerId;

    @NonNull
    private Throwable exception;

}
