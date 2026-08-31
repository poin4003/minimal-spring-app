package com.app.features.ai.rag.web.support;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.app.features.ai.rag.schema.model.PostRagStreamMetadata;
import com.app.features.ai.rag.service.PostRagStreamObserver;
import com.app.features.ai.rag.web.view.PostRagStreamCompletionView;
import com.app.features.ai.rag.web.view.PostRagStreamErrorView;
import com.app.features.ai.rag.web.view.PostRagStreamSourcesView;
import com.app.features.ai.rag.web.view.PostRagStreamTokenView;

public final class PostRagSseSession implements PostRagStreamObserver {

    private static final String CONNECTED_EVENT = "connected";
    private static final String SOURCES_EVENT = "sources";
    private static final String TOKEN_EVENT = "token";
    private static final String COMPLETE_EVENT = "complete";
    private static final String ERROR_EVENT = "error";

    private final SseEmitter emitter;
    private final PostRagChatSourceViewFactory chatSourceViewFactory;
    private final AtomicBoolean closed = new AtomicBoolean();

    public PostRagSseSession(
            SseEmitter emitter,
            PostRagChatSourceViewFactory chatSourceViewFactory) {
        this.emitter = emitter;
        this.chatSourceViewFactory = chatSourceViewFactory;
    }

    public void sendConnected() {
        send(SseEmitter.event()
                .name(CONNECTED_EVENT)
                .data(CONNECTED_EVENT));
    }

    @Override
    public void onMetadata(PostRagStreamMetadata metadata) {
        PostRagStreamSourcesView sources = PostRagStreamSourcesView.builder()
                .retrievalAvailability(metadata.retrievalAvailability())
                .generationAvailability(metadata.generationAvailability())
                .sources(metadata.sources().stream()
                        .map(source -> chatSourceViewFactory.build(source))
                        .toList())
                .build();

        send(SseEmitter.event()
                .name(SOURCES_EVENT)
                .data(sources, MediaType.APPLICATION_JSON));
    }

    @Override
    public void onToken(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }

        send(SseEmitter.event()
                .name(TOKEN_EVENT)
                .data(PostRagStreamTokenView.builder()
                        .text(token)
                        .build(), MediaType.APPLICATION_JSON));
    }

    public void sendCompletion(PostRagStreamCompletionView completion) {
        finish(SseEmitter.event()
                .name(COMPLETE_EVENT)
                .data(completion, MediaType.APPLICATION_JSON));
    }

    public void sendError(String message) {
        finish(SseEmitter.event()
                .name(ERROR_EVENT)
                .data(PostRagStreamErrorView.builder()
                        .message(message)
                        .build(), MediaType.APPLICATION_JSON));
    }

    public void sendHeartbeat() {
        send(SseEmitter.event().comment("heartbeat"));
    }

    @Override
    public boolean isCancelled() {
        return closed.get();
    }

    public void disconnect() {
        closed.set(true);
    }

    private synchronized void send(SseEmitter.SseEventBuilder event) {
        if (closed.get()) {
            return;
        }

        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            closed.set(true);
        }
    }

    private synchronized void finish(SseEmitter.SseEventBuilder event) {
        if (closed.get()) {
            return;
        }

        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            // The client has already disconnected; cancellation is enough.
        } finally {
            closed.set(true);
            try {
                emitter.complete();
            } catch (IllegalStateException exception) {
                // Completion can race with a client disconnect.
            }
        }
    }
}
