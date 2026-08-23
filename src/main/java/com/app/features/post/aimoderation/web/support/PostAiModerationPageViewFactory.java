package com.app.features.post.aimoderation.web.support;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.app.config.settings.AppProperties;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.post.aimoderation.entity.PostAiModerationDecisionLogEntity_;
import com.app.features.post.aimoderation.enums.PostAiModerationMode;
import com.app.features.post.aimoderation.enums.PostAiModerationOutcome;
import com.app.features.post.aimoderation.schema.result.PostAiModerationConfigResult;
import com.app.features.post.aimoderation.schema.result.PostAiModerationDecisionLogDetailResult;
import com.app.features.post.aimoderation.schema.result.PostAiModerationDecisionLogResult;
import com.app.features.post.aimoderation.service.PostAiModerationAdminService;
import com.app.features.post.aimoderation.web.view.PostAiModerationConfigForm;
import com.app.features.post.aimoderation.web.view.PostAiModerationConfigPageView;
import com.app.features.post.aimoderation.web.view.PostAiModerationDecisionLogModalView;
import com.app.features.post.aimoderation.web.view.PostAiModerationDecisionLogTableRowView;
import com.app.features.post.aimoderation.web.view.PostAiModerationLogPageView;
import com.app.features.post.aimoderation.web.view.PostAiModerationModeOptionView;
import com.app.features.post.aimoderation.web.view.PostAiModerationPanelState;
import com.app.features.post.aimoderation.web.view.PostAiModerationPanelView;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiTableFactory;
import com.app.features.ui.web.component.view.UiBreadcrumbItemView;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.component.view.UiTableActionView;
import com.app.features.ui.web.component.view.UiTableDefinition;
import com.app.features.ui.web.component.view.UiTableView;
import com.app.features.ui.web.support.UiDateTimeFormatter;
import com.app.features.ui.web.support.UiShellFactory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostAiModerationPageViewFactory {

    private static final String PANEL_ID = "post-ai-moderation-panel";
    private static final String LOG_TABLE_ID =
            "post-ai-moderation-log-table";
    private static final int DECISION_SUMMARY_LENGTH = 140;
    private static final UiPageDefaults LOG_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(10)
                    .sortBy(PostAiModerationDecisionLogEntity_.CREATED_AT)
                    .sortDirection(Sort.Direction.DESC)
                    .build();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final PostAiModerationAdminService postAiModerationAdminSvc;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiTableFactory uiTableFactory;
    private final UiShellFactory uiShellFactory;
    private final UiDateTimeFormatter dateTimeFormatter;
    private final ModelMapper mapper;

    public PostAiModerationConfigPageView buildConfigPage(
            UserPrincipal currentUser,
            String requestPath,
            PostAiModerationPanelState state) {
        return PostAiModerationConfigPageView.builder()
                .title(messageResolver.get(
                        "post.aiModeration.page.title"))
                .shell(uiShellFactory.build(currentUser, requestPath))
                .config(buildPanel(state))
                .build();
    }

    public PostAiModerationPanelView buildPanel(
            PostAiModerationPanelState state) {
        PostAiModerationConfigResult config =
                postAiModerationAdminSvc.getConfig();
        PostAiModerationConfigForm form = state != null
                && state.getForm() != null
                        ? state.getForm()
                        : mapper.map(
                                config,
                                PostAiModerationConfigForm.class);

        return PostAiModerationPanelView.builder()
                .id(PANEL_ID)
                .updatePath(getAiModerationPath() + "/config")
                .form(form)
                .modes(buildModeOptions())
                .fieldErrors(state == null
                        ? Map.of()
                        : state.getFieldErrors())
                .statusLabel(resolveModeLabel(config.getMode()))
                .updatedAt(dateTimeFormatter.format(
                        config.getUpdatedAt()))
                .saved(state != null && state.isSaved())
                .build();
    }

    public PostAiModerationLogPageView buildLogPage(
            UserPrincipal currentUser,
            String requestPath,
            UUID postId,
            UiPageQuery query) {
        Page<PostAiModerationDecisionLogResult> logPage =
                postAiModerationAdminSvc.getDecisionLogs(
                        postId,
                        query.toPageable(LOG_PAGE_DEFAULTS));
        List<PostAiModerationDecisionLogTableRowView> rows =
                logPage.getContent().stream()
                        .map(this::toTableRow)
                        .toList();
        UiPaginationView pagination = uiPaginationFactory.build(
                logPage,
                pageNumber -> buildLogPagePath(
                        postId,
                        query,
                        pageNumber));
        UiTableView table = uiTableFactory.build(
                UiTableDefinition.builder()
                        .id(LOG_TABLE_ID)
                        .title(messageResolver.get(
                                "post.aiModeration.log.title"))
                        .description(messageResolver.get(
                                "post.aiModeration.log.description"))
                        .emptyMessage(messageResolver.get(
                                "post.aiModeration.log.empty"))
                        .pagination(pagination)
                        .build(),
                rows,
                PostAiModerationDecisionLogTableRowView.class,
                this::buildLogActions);

        return PostAiModerationLogPageView.builder()
                .title(messageResolver.get(
                        "post.aiModeration.log.page.title"))
                .shell(uiShellFactory.build(currentUser, requestPath))
                .breadcrumb(buildLogBreadcrumb())
                .postId(postId)
                .moderationPath(getModerationPath())
                .logs(table)
                .build();
    }

    public PostAiModerationDecisionLogModalView buildDecisionLogModal(
            UUID postId,
            UUID logId) {
        PostAiModerationDecisionLogDetailResult log =
                postAiModerationAdminSvc.getDecisionLogDetail(
                        postId,
                        logId);

        return PostAiModerationDecisionLogModalView.builder()
                .id("post-ai-moderation-log-detail-modal")
                .title(messageResolver.get(
                        "post.aiModeration.log.detail.title"))
                .postId(log.getPostId().toString())
                .outcome(resolveOutcomeLabel(log.getOutcome()))
                .modelName(normalize(log.getModelName()))
                .createdAt(dateTimeFormatter.format(log.getCreatedAt()))
                .reason(normalize(log.getReason()))
                .errorMessage(normalize(log.getErrorMessage()))
                .promptSnapshot(normalize(log.getPromptSnapshot()))
                .rawResponse(normalize(log.getRawResponse()))
                .build();
    }

    private PostAiModerationDecisionLogTableRowView toTableRow(
            PostAiModerationDecisionLogResult log) {
        return PostAiModerationDecisionLogTableRowView.builder()
                .id(log.getId())
                .postId(log.getPostId())
                .outcome(log.getOutcome())
                .decisionSummary(summarize(StringUtils.hasText(
                        log.getErrorMessage())
                                ? log.getErrorMessage()
                                : log.getReason()))
                .modelName(log.getModelName())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private List<UiTableActionView> buildLogActions(
            PostAiModerationDecisionLogTableRowView row) {
        String logPagePath = getPostLogPath(row.getPostId());
        String detailPath = logPagePath + "/" + row.getId();

        return List.of(UiTableActionView.builder()
                .label(messageResolver.get("action.detail"))
                .path(logPagePath)
                .partialPath(detailPath)
                .buttonClass("btn-outline-primary")
                .build());
    }

    private UiBreadcrumbView buildLogBreadcrumb() {
        return UiBreadcrumbView.builder()
                .items(List.of(
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "post.moderation.page.title"))
                                .path(getModerationPath())
                                .build(),
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "post.aiModeration.log.page.title"))
                                .active(true)
                                .build()))
                .build();
    }

    private List<PostAiModerationModeOptionView> buildModeOptions() {
        return Arrays.stream(PostAiModerationMode.values())
                .map(mode -> PostAiModerationModeOptionView.builder()
                        .value(mode)
                        .label(resolveModeLabel(mode))
                        .build())
                .toList();
    }

    private String resolveModeLabel(PostAiModerationMode mode) {
        return messageResolver.get(
                "post.aiModeration.mode."
                        + mode.name().toLowerCase());
    }

    private String resolveOutcomeLabel(
            PostAiModerationOutcome outcome) {
        return messageResolver.get(
                "post.aiModeration.outcome."
                        + outcome.name().toLowerCase());
    }

    private String summarize(String value) {
        if (!StringUtils.hasText(value)) {
            return messageResolver.get("common.emptyValue");
        }

        String normalized = value.trim();
        if (normalized.length() <= DECISION_SUMMARY_LENGTH) {
            return normalized;
        }

        return normalized.substring(0, DECISION_SUMMARY_LENGTH - 3)
                + "...";
    }

    private String normalize(String value) {
        return StringUtils.hasText(value)
                ? value.trim()
                : messageResolver.get("common.emptyValue");
    }

    private String buildLogPagePath(
            UUID postId,
            UiPageQuery query,
            int pageNumber) {
        UiPageQuery pageQuery = query.applyDefaults(LOG_PAGE_DEFAULTS);
        pageQuery.setPage(pageNumber);
        return pageQuery.toUri(
                getPostLogPath(postId),
                LOG_PAGE_DEFAULTS);
    }

    private String getPostLogPath(UUID postId) {
        return getModerationPath()
                + "/"
                + postId
                + "/ai-logs";
    }

    private String getAiModerationPath() {
        return appProperties.getUi().getHomePath()
                + "/posts/ai-moderation";
    }

    private String getModerationPath() {
        return appProperties.getUi().getHomePath()
                + "/posts/moderation";
    }
}
