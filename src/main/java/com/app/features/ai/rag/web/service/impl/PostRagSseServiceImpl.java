package com.app.features.ai.rag.web.service.impl;

import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.app.config.settings.AppProperties;
import com.app.core.i18n.AppMessageResolver;
import com.app.features.ai.rag.schema.model.PostRagConversationRequest;
import com.app.features.ai.rag.schema.model.PostRagResult;
import com.app.features.ai.rag.service.PostRagService;
import com.app.features.ai.rag.web.service.PostRagSseService;
import com.app.features.ai.rag.web.support.PostRagChatMessageViewFactory;
import com.app.features.ai.rag.web.support.PostRagChatSourceViewFactory;
import com.app.features.ai.rag.web.support.PostRagSseSession;
import com.app.features.ai.rag.web.support.PostRagSseTaskExecutor;
import com.app.features.ai.rag.web.view.PostRagChatMessageView;
import com.app.features.ai.rag.web.view.PostRagStreamCompletionView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class PostRagSseServiceImpl implements PostRagSseService {

    private final Set<PostRagSseSession> activeSessions =
            ConcurrentHashMap.newKeySet();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final PostRagService postRagSvc;
    private final PostRagChatMessageViewFactory chatMessageViewFactory;
    private final PostRagChatSourceViewFactory chatSourceViewFactory;
    private final PostRagSseTaskExecutor taskExecutor;

    @Override
    public SseEmitter stream(PostRagConversationRequest request) {
        long timeout = appProperties.getAi()
                .getRag()
                .getStream()
                .getTimeout()
                .toMillis();
        SseEmitter emitter = new SseEmitter(timeout);
        PostRagSseSession session = new PostRagSseSession(
                emitter,
                chatSourceViewFactory);
        activeSessions.add(session);

        emitter.onCompletion(() -> close(session));
        emitter.onTimeout(() -> close(session));
        emitter.onError(error -> close(session));
        session.sendConnected();

        if (session.isCancelled()) {
            close(session);
            return emitter;
        }

        try {
            taskExecutor.execute(() -> process(request, session));
        } catch (TaskRejectedException exception) {
            activeSessions.remove(session);
            session.sendError(messageResolver.get("ai.chat.error.busy"));
        }

        return emitter;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.ai.rag.stream.heartbeat-interval:15s}")
    public void heartbeat() {
        activeSessions.forEach(session -> {
            session.sendHeartbeat();
            if (session.isCancelled()) {
                activeSessions.remove(session);
            }
        });
    }

    private void process(
            PostRagConversationRequest request,
            PostRagSseSession session) {
        try {
            PostRagResult result = postRagSvc.answer(request, session);
            if (session.isCancelled()) {
                return;
            }

            PostRagChatMessageView message =
                    chatMessageViewFactory.build(result);
            session.sendCompletion(PostRagStreamCompletionView.builder()
                    .generated(message.isGenerated())
                    .answer(message.isGenerated()
                            ? null
                            : message.getAnswer())
                    .build());
        } catch (CancellationException exception) {
            // Disconnecting the client is an expected cancellation path.
        } catch (RuntimeException exception) {
            log.warn("RAG SSE request failed.", exception);
            session.sendError(messageResolver.get("ai.chat.error.request"));
        } finally {
            activeSessions.remove(session);
        }
    }

    private void close(PostRagSseSession session) {
        session.disconnect();
        activeSessions.remove(session);
    }
}
