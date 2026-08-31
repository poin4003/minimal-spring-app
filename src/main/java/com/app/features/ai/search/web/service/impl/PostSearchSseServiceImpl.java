package com.app.features.ai.search.web.service.impl;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.app.config.settings.AppProperties;
import com.app.core.enums.AppLanguage;
import com.app.core.i18n.AppMessageResolver;
import com.app.features.ai.enums.AiAvailability;
import com.app.features.ai.search.schema.model.PostSearchRequest;
import com.app.features.ai.search.schema.model.PostSearchResult;
import com.app.features.ai.search.schema.model.PostSearchSummaryRequest;
import com.app.features.ai.search.schema.model.PostSearchSummaryResult;
import com.app.features.ai.search.service.PostSearchService;
import com.app.features.ai.search.service.PostSearchSummaryService;
import com.app.features.ai.search.web.service.PostSearchSseService;
import com.app.features.ai.search.web.support.PostSearchItemViewFactory;
import com.app.features.ai.search.web.support.PostSearchSseSession;
import com.app.features.ai.search.web.support.PostSearchSseTaskExecutor;
import com.app.features.ai.search.web.view.PostSearchItemView;
import com.app.features.ai.search.web.view.PostSearchStreamCompletionView;
import com.app.features.ai.search.web.view.PostSearchStreamResultsView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class PostSearchSseServiceImpl implements PostSearchSseService {

    private final Set<PostSearchSseSession> activeSessions =
            ConcurrentHashMap.newKeySet();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final PostSearchService postSearchSvc;
    private final PostSearchSummaryService postSearchSummarySvc;
    private final PostSearchItemViewFactory postSearchItemViewFactory;
    private final PostSearchSseTaskExecutor taskExecutor;

    @Override
    public SseEmitter stream(
            String query,
            AppLanguage responseLanguage,
            boolean summarize) {
        long timeout = appProperties.getAi()
                .getRag()
                .getStream()
                .getTimeout()
                .toMillis();
        SseEmitter emitter = new SseEmitter(timeout);
        PostSearchSseSession session = new PostSearchSseSession(emitter);
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
            taskExecutor.execute(() -> process(
                    query,
                    responseLanguage,
                    summarize,
                    session));
        } catch (TaskRejectedException exception) {
            activeSessions.remove(session);
            session.sendError(messageResolver.get("ai.search.error.busy"));
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
            String query,
            AppLanguage responseLanguage,
            boolean summarize,
            PostSearchSseSession session) {
        try {
            PostSearchResult searchResult = postSearchSvc.search(
                    new PostSearchRequest(
                            query,
                            null,
                            appProperties.getAi().getSearch()
                                    .getDefaultLimit()));
            AiAvailability summaryAvailability =
                    postSearchSummarySvc.resolveAvailability();
            List<PostSearchItemView> items = searchResult.items().stream()
                    .map(item -> postSearchItemViewFactory.build(item))
                    .toList();
            session.sendResults(PostSearchStreamResultsView.builder()
                    .retrievalAvailability(searchResult.availability())
                    .summaryAvailability(summaryAvailability)
                    .items(items)
                    .build());

            if (session.isCancelled()) {
                return;
            }
            if (searchResult.availability() != AiAvailability.READY
                    || searchResult.items().isEmpty()
                    || !summarize
                    || summaryAvailability != AiAvailability.READY) {
                session.sendCompletion(PostSearchStreamCompletionView.builder()
                        .summarized(false)
                        .build());
                return;
            }

            PostSearchSummaryResult summaryResult =
                    postSearchSummarySvc.summarize(
                            new PostSearchSummaryRequest(
                                    searchResult,
                                    responseLanguage),
                            session);
            if (session.isCancelled()) {
                return;
            }
            session.sendCompletion(PostSearchStreamCompletionView.builder()
                    .summarized(summaryResult.isGenerated())
                    .build());
        } catch (CancellationException exception) {
            // Disconnecting the client is an expected cancellation path.
        } catch (RuntimeException exception) {
            log.warn("Semantic search SSE request failed.", exception);
            session.sendError(messageResolver.get("ai.search.error.request"));
        } finally {
            activeSessions.remove(session);
        }
    }

    private void close(PostSearchSseSession session) {
        session.disconnect();
        activeSessions.remove(session);
    }
}
