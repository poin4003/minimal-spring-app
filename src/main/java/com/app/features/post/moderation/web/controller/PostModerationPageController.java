package com.app.features.post.moderation.web.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.security.web.HtmxRequestSupport;
import com.app.config.settings.AppProperties;
import com.app.core.constant.PermissionConstants;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.post.entity.PostEntity_;
import com.app.features.post.enums.PostType;
import com.app.features.post.moderation.enums.PostModerationStatus;
import com.app.features.post.moderation.schema.filter.ModerationPostFilterCriteria;
import com.app.features.post.moderation.schema.payload.RejectPostPayload;
import com.app.features.post.moderation.schema.result.ModerationPostResult;
import com.app.features.post.moderation.schema.result.ModerationPostDetailResult;
import com.app.features.post.moderation.schema.result.ModerationShortPostDetailResult;
import com.app.features.post.moderation.schema.result.ModerationStandardPostDetailResult;
import com.app.features.post.moderation.schema.result.ModerationVideoPostDetailResult;
import com.app.features.post.moderation.service.PostModerationService;
import com.app.features.post.moderation.web.view.ModerationPostDetailPageView;
import com.app.features.post.moderation.web.view.ModerationPostDetailView;
import com.app.features.post.moderation.web.view.ModerationPostListPageView;
import com.app.features.post.moderation.web.view.ModerationPostQueueView;
import com.app.features.post.moderation.web.view.ModerationPostTableRowView;
import com.app.features.post.moderation.web.view.ModerationPostTypeOptionView;
import com.app.features.post.moderation.web.view.RejectPostModalForm;
import com.app.features.ui.web.component.support.UiModalFactory;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiTableFactory;
import com.app.features.ui.web.component.view.UiBreadcrumbItemView;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.component.view.UiConfirmModalView;
import com.app.features.ui.web.component.view.UiHtmxNavigationView;
import com.app.features.ui.web.component.view.UiModalDefinition;
import com.app.features.ui.web.component.view.UiModalView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.component.view.UiTableActionView;
import com.app.features.ui.web.component.view.UiTableDefinition;
import com.app.features.ui.web.component.view.UiTableView;
import com.app.features.ui.web.support.UiFormSubmitResult;
import com.app.features.ui.web.support.UiFormSubmitSupport;
import com.app.features.ui.web.support.UiShellFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/posts/moderation")
@Secured(PermissionConstants.POST_MODERATE)
public class PostModerationPageController {

    private static final String QUEUE_ID = "post-moderation-queue";
    private static final String TABLE_ID = "post-moderation-table";
    private static final String QUEUE_CHANGED_EVENT =
            "postModerationQueueChanged";
    private static final String EMPTY_RESPONSE_VIEW =
            "fragments/components/htmx-response :: empty";

    private static final UiPageDefaults PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(10)
                    .sortBy(PostEntity_.CREATED_AT)
                    .sortDirection(Sort.Direction.ASC)
                    .build();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final UiShellFactory uiShellFactory;
    private final PostModerationService postModerationSvc;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiTableFactory uiTableFactory;
    private final UiModalFactory uiModalFactory;
    private final UiFormSubmitSupport uiFormSubmitSupport;
    private final ModelMapper mapper;

    @GetMapping
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter")
            ModerationPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        ModerationPostListPageView page =
                ModerationPostListPageView.builder()
                        .title(messageResolver.get(
                                "post.moderation.page.title"))
                        .listPath(getModerationPath())
                        .shell(uiShellFactory.build(
                                currentUser,
                                request.getRequestURI()))
                        .filter(filter)
                        .query(query.applyDefaults(PAGE_DEFAULTS))
                        .postTypes(buildPostTypeOptions())
                        .queue(buildQueue(filter, query))
                        .build();

        model.addAttribute(ModerationPostListPageView.ATTRIBUTE, page);
        return "post/moderation/index";
    }

    @GetMapping("/queue")
    public String queue(
            @Valid @ModelAttribute("filter")
            ModerationPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        model.addAttribute(
                ModerationPostQueueView.ATTRIBUTE,
                buildQueue(filter, query));
        return "post/moderation/fragments/queue :: queue (queue=${queue})";
    }

    @GetMapping("/{postId}")
    public String detail(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter")
            ModerationPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        model.addAttribute(
                ModerationPostDetailPageView.ATTRIBUTE,
                buildDetailPage(
                        currentUser,
                        request,
                        postId,
                        filter,
                        query,
                        null,
                        null,
                        null));
        return "post/moderation/detail";
    }

    @GetMapping("/{postId}/publish-confirm")
    public String publishConfirm(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter")
            ModerationPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @RequestParam(defaultValue = "false") boolean detail,
            Model model) {
        UiConfirmModalView publishModal = buildPublishModal(
                postId,
                filter,
                query,
                detail);
        if (HtmxRequestSupport.isHtmxRequest(request)) {
            model.addAttribute(
                    UiConfirmModalView.ATTRIBUTE,
                    publishModal);
            return "fragments/components/confirm-modal"
                    + " :: modal (modal=${modal})";
        }

        model.addAttribute(
                ModerationPostDetailPageView.ATTRIBUTE,
                buildDetailPage(
                        currentUser,
                        request,
                        postId,
                        filter,
                        query,
                        publishModal,
                        null,
                        publishModal.getId()));
        return "post/moderation/detail";
    }

    @GetMapping("/{postId}/reject")
    public String rejectModal(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter")
            ModerationPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @RequestParam(defaultValue = "false") boolean detail,
            Model model) {
        UiModalView rejectModal = buildRejectModal(
                postId,
                filter,
                query,
                new RejectPostModalForm(),
                Map.of(),
                detail);
        if (HtmxRequestSupport.isHtmxRequest(request)) {
            model.addAttribute(UiModalView.ATTRIBUTE, rejectModal);
            return "fragments/components/modal :: modal (modal=${modal})";
        }

        model.addAttribute(
                ModerationPostDetailPageView.ATTRIBUTE,
                buildDetailPage(
                        currentUser,
                        request,
                        postId,
                        filter,
                        query,
                        null,
                        rejectModal,
                        rejectModal.getId()));
        return "post/moderation/detail";
    }

    @PostMapping("/{postId}/publish")
    public String publish(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @ModelAttribute("filter")
            ModerationPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @RequestParam(defaultValue = "false") boolean detail) {
        postModerationSvc.publishPost(postId, currentUser.getUserId());
        return actionSucceeded(request, response, filter, query, detail);
    }

    @PostMapping("/{postId}/reject")
    public String reject(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @ModelAttribute("filter")
            ModerationPostFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @RequestParam(defaultValue = "false") boolean detail,
            @Valid @ModelAttribute("form") RejectPostModalForm form,
            BindingResult bindingResult,
            Model model) {
        UiFormSubmitResult submitResult = uiFormSubmitSupport.submit(
                bindingResult,
                () -> postModerationSvc.rejectPost(
                        postId,
                        currentUser.getUserId(),
                        mapper.map(form, RejectPostPayload.class)));

        if (submitResult.success()) {
            return actionSucceeded(
                    request,
                    response,
                    filter,
                    query,
                    detail);
        }

        if (HtmxRequestSupport.isHtmxRequest(request)) {
            model.addAttribute(
                    UiModalView.ATTRIBUTE,
                    buildRejectModal(
                            postId,
                            filter,
                            query,
                            form,
                            submitResult.fieldErrors(),
                            detail));
            return "fragments/components/modal :: modal (modal=${modal})";
        }

        UiModalView rejectModal = buildRejectModal(
                postId,
                filter,
                query,
                form,
                submitResult.fieldErrors(),
                detail);
        model.addAttribute(
                ModerationPostDetailPageView.ATTRIBUTE,
                buildDetailPage(
                        currentUser,
                        request,
                        postId,
                        filter,
                        query,
                        null,
                        rejectModal,
                        rejectModal.getId()));
        return "post/moderation/detail";
    }

    private ModerationPostQueueView buildQueue(
            ModerationPostFilterCriteria filter,
            UiPageQuery query) {
        Page<ModerationPostResult> postPage =
                postModerationSvc.getPendingPosts(
                        filter,
                        query.toPageable(PAGE_DEFAULTS));
        List<ModerationPostTableRowView> rows =
                postPage.getContent().stream()
                        .map(post -> toTableRow(post))
                        .toList();
        UiPaginationView pagination = uiPaginationFactory.build(
                postPage,
                pageNumber -> buildPagePath(
                        getModerationPath(),
                        filter,
                        query,
                        pageNumber),
                UiHtmxNavigationView.forComponent(QUEUE_ID));
        UiTableView table = uiTableFactory.build(
                UiTableDefinition.builder()
                        .id(TABLE_ID)
                        .title(messageResolver.get(
                                "post.moderation.queue.title"))
                        .description(messageResolver.get(
                                "post.moderation.queue.description"))
                        .emptyMessage(messageResolver.get(
                                "post.moderation.queue.empty"))
                        .pagination(pagination)
                        .build(),
                rows,
                ModerationPostTableRowView.class,
                row -> buildRowActions(row, filter, query));

        return ModerationPostQueueView.builder()
                .id(QUEUE_ID)
                .refreshPath(buildStatePath(
                        getModerationPath() + "/queue",
                        filter,
                        query))
                .refreshEvent(QUEUE_CHANGED_EVENT)
                .table(table)
                .build();
    }

    private ModerationPostTableRowView toTableRow(
            ModerationPostResult post) {
        String authorName = post.getAuthor() == null
                || post.getAuthor().getFullName() == null
                || post.getAuthor().getFullName().isBlank()
                        ? messageResolver.get("post.public.unknownAuthor")
                        : post.getAuthor().getFullName();

        return ModerationPostTableRowView.builder()
                .id(post.getId())
                .type(post.getType())
                .typeLabel(resolveTypeLabel(post.getType()))
                .authorName(authorName)
                .moderationStatusLabel(resolveStatusLabel(
                        post.getModerationStatus()))
                .createdAt(post.getCreatedAt())
                .build();
    }

    private List<UiTableActionView> buildRowActions(
            ModerationPostTableRowView row,
            ModerationPostFilterCriteria filter,
            UiPageQuery query) {
        String detailPath = buildStatePath(
                buildPostPath(row.getId()),
                filter,
                query);
        String publishModalPath = buildStatePath(
                buildPostPath(row.getId(), "publish-confirm"),
                filter,
                query);
        String rejectModalPath = buildStatePath(
                buildPostPath(row.getId(), "reject"),
                filter,
                query);

        return List.of(
                UiTableActionView.builder()
                        .label(messageResolver.get("action.review"))
                        .path(detailPath)
                        .buttonClass("btn-outline-primary")
                        .build(),
                UiTableActionView.builder()
                        .label(messageResolver.get("action.publish"))
                        .path(publishModalPath)
                        .partialPath(publishModalPath)
                        .buttonClass("btn-outline-success")
                        .build(),
                UiTableActionView.builder()
                        .label(messageResolver.get("action.reject"))
                        .path(rejectModalPath)
                        .partialPath(rejectModalPath)
                        .buttonClass("btn-outline-danger")
                        .build());
    }

    private ModerationPostDetailPageView buildDetailPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            UUID postId,
            ModerationPostFilterCriteria filter,
            UiPageQuery query,
            UiConfirmModalView publishModal,
            UiModalView rejectModal,
            String openModalId) {
        ModerationPostDetailView post = toDetailView(
                postModerationSvc.getPostDetail(postId));
        String queuePath = buildStatePath(
                getModerationPath(),
                filter,
                query);
        UiConfirmModalView detailPublishModal = publishModal != null
                ? publishModal
                : buildPublishModal(postId, filter, query, true);
        UiModalView detailRejectModal = rejectModal != null
                ? rejectModal
                : buildRejectModal(
                        postId,
                        filter,
                        query,
                        new RejectPostModalForm(),
                        Map.of(),
                        true);

        return ModerationPostDetailPageView.builder()
                .title(messageResolver.get(
                        "post.moderation.detail.title"))
                .shell(uiShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .breadcrumb(buildDetailBreadcrumb(queuePath))
                .post(post)
                .statusLabel(resolveStatusLabel(
                        post.getState().getModerationStatus()))
                .queuePath(queuePath)
                .refreshEvent(QUEUE_CHANGED_EVENT)
                .publishModalPath(buildStatePath(
                        buildPostPath(postId, "publish-confirm"),
                        filter,
                        query,
                        true))
                .rejectModalPath(buildStatePath(
                        buildPostPath(postId, "reject"),
                        filter,
                        query,
                        true))
                .publishModal(detailPublishModal)
                .rejectModal(detailRejectModal)
                .openModalId(openModalId)
                .build();
    }

    private ModerationPostDetailView toDetailView(
            ModerationPostDetailResult post) {
        if (post instanceof ModerationStandardPostDetailResult standardPost) {
            return ModerationPostDetailView.builder()
                    .post(standardPost.getPost())
                    .state(standardPost.getState())
                    .content(standardPost.getContent())
                    .media(standardPost.getMedia())
                    .build();
        }

        if (post instanceof ModerationShortPostDetailResult shortPost) {
            return ModerationPostDetailView.builder()
                    .post(shortPost.getPost())
                    .state(shortPost.getState())
                    .content(shortPost.getCaption())
                    .media(List.of(shortPost.getMedia()))
                    .build();
        }

        ModerationVideoPostDetailResult videoPost =
                (ModerationVideoPostDetailResult) post;
        return ModerationPostDetailView.builder()
                .post(videoPost.getPost())
                .state(videoPost.getState())
                .title(videoPost.getTitle())
                .content(videoPost.getDescription())
                .media(List.of(videoPost.getContent()))
                .build();
    }

    private UiConfirmModalView buildPublishModal(
            UUID postId,
            ModerationPostFilterCriteria filter,
            UiPageQuery query,
            boolean detail) {
        return UiConfirmModalView.builder()
                .id("post-publish-modal")
                .title(messageResolver.get(
                        "post.moderation.publish.title"))
                .description(messageResolver.get(
                        "post.moderation.publish.description"))
                .actionPath(buildStatePath(
                        buildPostPath(postId, "publish"),
                        filter,
                        query,
                        detail))
                .confirmLabel(messageResolver.get("action.publish"))
                .confirmButtonClass("btn-success")
                .build();
    }

    private UiModalView buildRejectModal(
            UUID postId,
            ModerationPostFilterCriteria filter,
            UiPageQuery query,
            RejectPostModalForm form,
            Map<String, String> fieldErrors,
            boolean detail) {
        return uiModalFactory.build(
                UiModalDefinition.builder()
                        .id("post-reject-modal")
                        .title(messageResolver.get(
                                "post.moderation.reject.title"))
                        .description(messageResolver.get(
                                "post.moderation.reject.description"))
                        .actionPath(buildStatePath(
                                buildPostPath(postId, "reject"),
                                filter,
                                query,
                                detail))
                        .submitLabel(messageResolver.get("action.reject"))
                        .build(),
                RejectPostModalForm.class,
                form,
                Map.of(),
                fieldErrors);
    }

    private UiBreadcrumbView buildDetailBreadcrumb(String queuePath) {
        return UiBreadcrumbView.builder()
                .items(List.of(
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "post.moderation.page.title"))
                                .path(queuePath)
                                .build(),
                        UiBreadcrumbItemView.builder()
                                .label(messageResolver.get(
                                        "post.moderation.detail.title"))
                                .active(true)
                                .build()))
                .build();
    }

    private List<ModerationPostTypeOptionView> buildPostTypeOptions() {
        return Arrays.stream(PostType.values())
                .map(type -> ModerationPostTypeOptionView.builder()
                        .value(type)
                        .label(resolveTypeLabel(type))
                        .build())
                .toList();
    }

    private String resolveTypeLabel(PostType type) {
        return messageResolver.get(
                "post.type." + type.name().toLowerCase());
    }

    private String resolveStatusLabel(PostModerationStatus status) {
        return switch (status) {
            case PENDING_REVIEW -> messageResolver.get(
                    "post.moderationStatus.pendingReview");
            case PUBLISHED -> messageResolver.get(
                    "post.moderationStatus.published");
            case REJECTED -> messageResolver.get(
                    "post.moderationStatus.rejected");
        };
    }

    private String actionSucceeded(
            HttpServletRequest request,
            HttpServletResponse response,
            ModerationPostFilterCriteria filter,
            UiPageQuery query,
            boolean detail) {
        String queuePath = buildStatePath(
                getModerationPath(),
                filter,
                query);
        if (HtmxRequestSupport.isHtmxRequest(request)) {
            if (detail) {
                HtmxRequestSupport.redirect(response, queuePath);
                return EMPTY_RESPONSE_VIEW;
            }
            HtmxRequestSupport.trigger(response, QUEUE_CHANGED_EVENT);
            return EMPTY_RESPONSE_VIEW;
        }

        return "redirect:" + queuePath;
    }

    private String buildStatePath(
            String basePath,
            ModerationPostFilterCriteria filter,
            UiPageQuery query) {
        return buildStatePath(basePath, filter, query, false);
    }

    private String buildStatePath(
            String basePath,
            ModerationPostFilterCriteria filter,
            UiPageQuery query,
            boolean detail) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
                query.toUri(basePath, PAGE_DEFAULTS));

        if (filter.getType() != null) {
            builder.queryParam("type", filter.getType());
        }
        if (filter.getAuthorId() != null) {
            builder.queryParam("authorId", filter.getAuthorId());
        }
        if (filter.getCreatedFrom() != null) {
            builder.queryParam("createdFrom", filter.getCreatedFrom());
        }
        if (filter.getCreatedTo() != null) {
            builder.queryParam("createdTo", filter.getCreatedTo());
        }
        if (detail) {
            builder.queryParam("detail", true);
        }

        return builder.build().encode().toUriString();
    }

    private String buildPagePath(
            String basePath,
            ModerationPostFilterCriteria filter,
            UiPageQuery query,
            int pageNumber) {
        UiPageQuery pageQuery = query.applyDefaults(PAGE_DEFAULTS);
        pageQuery.setPage(pageNumber);
        return buildStatePath(basePath, filter, pageQuery);
    }

    private String buildPostPath(UUID postId, String... segments) {
        StringBuilder path = new StringBuilder(getModerationPath())
                .append('/')
                .append(postId);
        for (String segment : segments) {
            path.append('/').append(segment);
        }
        return path.toString();
    }

    private String getModerationPath() {
        return appProperties.getUi().getHomePath()
                + "/posts/moderation";
    }
}
