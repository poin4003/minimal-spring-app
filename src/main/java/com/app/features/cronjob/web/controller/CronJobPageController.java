package com.app.features.cronjob.web.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.modelmapper.ModelMapper;
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

import com.app.config.settings.AppProperties;
import com.app.config.security.web.HtmxRequestSupport;
import com.app.core.constant.PermissionConstants;
import com.app.core.enums.RecordStatus;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.cronjob.schema.result.CronJobDetailResult;
import com.app.features.cronjob.service.CronJobService;
import com.app.features.cronjob.web.view.CronJobDetailModalForm;
import com.app.features.cronjob.web.view.CronJobListPageView;
import com.app.features.cronjob.web.view.CronJobTableRowView;
import com.app.features.ui.web.component.support.UiModalFactory;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.support.UiTableFactory;
import com.app.features.ui.web.component.view.UiMetadataItemView;
import com.app.features.ui.web.component.view.UiHtmxNavigationView;
import com.app.features.ui.web.component.view.UiMetadataModalView;
import com.app.features.ui.web.component.view.UiModalDefinition;
import com.app.features.ui.web.component.view.UiModalFieldOptionView;
import com.app.features.ui.web.component.view.UiModalView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.component.view.UiTableActionView;
import com.app.features.ui.web.component.view.UiTableDefinition;
import com.app.features.ui.web.component.view.UiTableView;
import com.app.features.ui.web.support.UiFormSubmitResult;
import com.app.features.ui.web.support.UiFormSubmitSupport;
import com.app.features.ui.web.support.UiDateTimeFormatter;
import com.app.features.ui.web.support.UiShellFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/cronjobs")
public class CronJobPageController {

    private static final String CRONJOB_TABLE_ID = "cronjob-table";

    private static final UiPageDefaults CRONJOB_PAGE_DEFAULTS = UiPageDefaults.builder()
            .page(0)
            .size(10)
            .sortBy("updatedAt")
            .sortDirection(Sort.Direction.DESC)
            .build();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final UiShellFactory uiShellFactory;
    private final CronJobService cronJobSvc;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;
    private final UiTableFactory uiTableFactory;
    private final UiModalFactory uiModalFactory;
    private final UiFormSubmitSupport uiFormSubmitSupport;
    private final UiDateTimeFormatter dateTimeFormatter;
    private final ModelMapper mapper;

    @GetMapping
    @Secured(PermissionConstants.CRONJOB_VIEW)
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @RequestParam(required = false) String metadataJobType,
            @RequestParam(required = false) String detailJobType,
            Model model) {
        model.addAttribute(
                CronJobListPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        query,
                        new CronJobDetailModalForm(),
                        null,
                        null,
                        metadataJobType,
                        metadataJobType != null,
                        detailJobType,
                        detailJobType != null,
                        false));
        return "cronjob/index";
    }

    @GetMapping("/{jobType}/metadata")
    @Secured(PermissionConstants.CRONJOB_VIEW)
    public String metadata(
            @PathVariable String jobType,
            Model model) {
        model.addAttribute(
                UiMetadataModalView.ATTRIBUTE,
                buildMetadataModal(jobType));
        return "fragments/components/metadata-modal :: modal (modal=${modal})";
    }

    @GetMapping("/{jobType}/detail")
    @Secured(PermissionConstants.CRONJOB_VIEW)
    public String detail(
            @PathVariable String jobType,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        model.addAttribute(
                UiModalView.ATTRIBUTE,
                buildDetailModal(
                        jobType,
                        query,
                        new CronJobDetailModalForm(),
                        Map.of(),
                        false));
        return "fragments/components/modal :: modal (modal=${modal})";
    }

    @PostMapping("/{jobType}")
    @Secured(PermissionConstants.CRONJOB_UPDATE)
    public String update(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String jobType,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @Valid @ModelAttribute("form") CronJobDetailModalForm form,
            BindingResult bindingResult,
            Model model) {
        UiFormSubmitResult submitResult = uiFormSubmitSupport.submit(
                bindingResult,
                () -> cronJobSvc.updateConfig(jobType, form.getCronExpression(), form.getStatus()));

        if (submitResult.success()) {
            return HtmxRequestSupport.redirectView(
                    request,
                    response,
                    query.toUri(
                            appProperties.getUi().getHomePath() + "/cronjobs",
                            CRONJOB_PAGE_DEFAULTS));
        }

        if (HtmxRequestSupport.isHtmxRequest(request)) {
            model.addAttribute(
                    UiModalView.ATTRIBUTE,
                    buildDetailModal(
                            jobType,
                            query,
                            form,
                            submitResult.fieldErrors(),
                            true));
            return "fragments/components/modal :: modal (modal=${modal})";
        }

        model.addAttribute(
                CronJobListPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        query,
                        form,
                        submitResult.fieldErrors(),
                        messageResolver.get("form.validation.correct"),
                        null,
                        false,
                        jobType,
                        true,
                        true));
        return "cronjob/index";
    }

    private CronJobListPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            UiPageQuery query,
            CronJobDetailModalForm form,
            Map<String, String> modalErrors,
            String errorMessage,
            String metadataJobType,
            boolean openMetadataModal,
            String detailJobType,
            boolean openDetailModal,
            boolean preserveDetailForm) {
        var cronJobPage = cronJobSvc.getManyCronJobs(query.toPageable(CRONJOB_PAGE_DEFAULTS));
        List<CronJobTableRowView> rows = cronJobPage.getContent().stream()
                .map(result -> mapper.map(result, CronJobTableRowView.class))
                .toList();

        UiPaginationView pagination = uiPaginationFactory.build(
                cronJobPage,
                uiPaginationPathBuilder.build(request, query, CRONJOB_PAGE_DEFAULTS),
                UiHtmxNavigationView.forComponent(CRONJOB_TABLE_ID));

        UiTableView cronJobTable = uiTableFactory.build(
                UiTableDefinition.builder()
                        .id(CRONJOB_TABLE_ID)
                        .title(messageResolver.get("cronjob.table.title"))
                        .description(messageResolver.get("cronjob.table.description"))
                        .emptyMessage(messageResolver.get("cronjob.table.empty"))
                        .pagination(pagination)
                        .build(),
                rows,
                CronJobTableRowView.class,
                row -> List.of(
                        UiTableActionView.builder()
                                .label(messageResolver.get("action.metadata"))
                                .path(buildMetadataPath(row.getJobType(), query))
                                .partialPath(buildMetadataPartialPath(row.getJobType()))
                                .buttonClass("btn-outline-secondary")
                                .build(),
                        UiTableActionView.builder()
                                .label(messageResolver.get("action.detail"))
                                .path(buildDetailPath(row.getJobType(), query))
                                .partialPath(buildDetailPartialPath(row.getJobType(), query))
                                .buttonClass("btn-primary")
                                .build()));

        UiMetadataModalView metadataModal = metadataJobType == null
                ? null
                : buildMetadataModal(metadataJobType);

        UiModalView detailModal = detailJobType == null
                ? null
                : buildDetailModal(detailJobType, query, form, modalErrors, preserveDetailForm);

        return CronJobListPageView.builder()
                .title(messageResolver.get("cronjob.page.title"))
                .shell(uiShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .cronJobTable(cronJobTable)
                .metadataModal(metadataModal)
                .detailModal(detailModal)
                .errorMessage(errorMessage)
                .openMetadataModal(openMetadataModal && metadataModal != null)
                .openDetailModal(openDetailModal && detailModal != null)
                .build();
    }

    private UiMetadataModalView buildMetadataModal(String jobType) {
        CronJobDetailResult cronJob = cronJobSvc.getCronJobDetail(jobType);

        return UiMetadataModalView.builder()
                .id("cronjob-metadata-modal")
                .title(messageResolver.get("cronjob.metadata.title"))
                .items(List.of(
                        item(messageResolver.get("field.id"), String.valueOf(cronJob.getId()), true),
                        item(messageResolver.get("field.jobType"), cronJob.getJobType(), true),
                        item(
                                messageResolver.get("field.cronOverride"),
                                cronJob.isUsingDefaultCron()
                                        ? messageResolver.get("cronjob.usingDefaultCron")
                                        : cronJob.getCronExpression(),
                                true),
                        item(messageResolver.get("field.status"), String.valueOf(cronJob.getStatus()), false),
                        item(
                                messageResolver.get("field.createdAt"),
                                dateTimeFormatter.format(cronJob.getCreatedAt()),
                                true),
                        item(
                                messageResolver.get("field.updatedAt"),
                                dateTimeFormatter.format(cronJob.getUpdatedAt()),
                                true)))
                .build();
    }

    private UiModalView buildDetailModal(
            String jobType,
            UiPageQuery query,
            CronJobDetailModalForm form,
            Map<String, String> modalErrors,
            boolean preserveDetailForm) {
        CronJobDetailModalForm modalForm = preserveDetailForm
                ? form
                : buildDetailForm(jobType);

        return uiModalFactory.build(
                UiModalDefinition.builder()
                        .id("cronjob-detail-modal")
                        .title(messageResolver.get("cronjob.detail.title"))
                        .description(messageResolver.get("cronjob.detail.description"))
                        .actionPath(query.toUri(
                                appProperties.getUi().getHomePath() + "/cronjobs/" + jobType,
                                CRONJOB_PAGE_DEFAULTS))
                        .submitLabel(messageResolver.get("action.saveChanges"))
                        .build(),
                CronJobDetailModalForm.class,
                modalForm,
                Map.of("status", buildStatusOptions(modalForm.getStatus())),
                modalErrors == null ? Map.of() : modalErrors);
    }

    private CronJobDetailModalForm buildDetailForm(String jobType) {
        CronJobDetailResult cronJob = cronJobSvc.getCronJobDetail(jobType);

        CronJobDetailModalForm form = new CronJobDetailModalForm();
        form.setJobType(cronJob.getJobType());
        form.setName(cronJob.getName());
        form.setDefaultCron(cronJob.getDefaultCron());
        form.setEffectiveCron(cronJob.getEffectiveCron());
        form.setZoneId(cronJob.getZoneId());
        form.setCreatedAt(cronJob.getCreatedAt());
        form.setUpdatedAt(cronJob.getUpdatedAt());
        form.setCronExpression(cronJob.getCronExpression());
        form.setStatus(cronJob.getStatus());
        return form;
    }

    private List<UiModalFieldOptionView> buildStatusOptions(RecordStatus selectedStatus) {
        return Arrays.stream(RecordStatus.values())
                .map(status -> UiModalFieldOptionView.builder()
                        .value(status.name())
                        .label(status.name())
                        .selected(status == selectedStatus)
                        .build())
                .toList();
    }

    private UiMetadataItemView item(String label, String value, boolean monospace) {
        return UiMetadataItemView.builder()
                .label(label)
                .value(value)
                .monospace(monospace)
                .build();
    }

    private String buildMetadataPath(String jobType, UiPageQuery query) {
        return UriComponentsBuilder.fromUriString(query.toUri(
                appProperties.getUi().getHomePath() + "/cronjobs",
                CRONJOB_PAGE_DEFAULTS))
                .replaceQueryParam("detailJobType")
                .replaceQueryParam("metadataJobType", jobType)
                .build()
                .encode()
                .toUriString();
    }

    private String buildMetadataPartialPath(String jobType) {
        return appProperties.getUi().getHomePath()
                + "/cronjobs/"
                + jobType
                + "/metadata";
    }

    private String buildDetailPath(String jobType, UiPageQuery query) {
        return UriComponentsBuilder.fromUriString(query.toUri(
                appProperties.getUi().getHomePath() + "/cronjobs",
                CRONJOB_PAGE_DEFAULTS))
                .replaceQueryParam("metadataJobType")
                .replaceQueryParam("detailJobType", jobType)
                .build()
                .encode()
                .toUriString();
    }

    private String buildDetailPartialPath(String jobType, UiPageQuery query) {
        return query.toUri(
                appProperties.getUi().getHomePath()
                        + "/cronjobs/"
                        + jobType
                        + "/detail",
                CRONJOB_PAGE_DEFAULTS);
    }

}
