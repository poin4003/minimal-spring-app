package com.app.features.cronjob.web.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
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
import com.app.core.menu.MenuService;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.cronjob.enums.CronjobStatusEnum;
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
import com.app.features.ui.web.view.UiCurrentUserView;
import com.app.features.ui.web.view.UiShellView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/cronjobs")
public class CronJobPageController {

    private static final UiPageDefaults CRONJOB_PAGE_DEFAULTS = UiPageDefaults.builder()
            .page(0)
            .size(10)
            .sortBy("updatedAt")
            .sortDirection(Sort.Direction.DESC)
            .build();

    private final AppProperties appProperties;
    private final MenuService menuService;
    private final CronJobService cronJobService;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;
    private final UiTableFactory uiTableFactory;
    private final UiModalFactory uiModalFactory;
    private final UiFormSubmitSupport uiFormSubmitSupport;
    private final ModelMapper mapper;

    @GetMapping
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

    @PostMapping("/{jobType}")
    public String update(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @PathVariable String jobType,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @Valid @ModelAttribute("form") CronJobDetailModalForm form,
            BindingResult bindingResult,
            Model model) {
        UiFormSubmitResult submitResult = uiFormSubmitSupport.submit(
                bindingResult,
                () -> cronJobService.updateConfig(jobType, form.getCronExpression(), form.getStatus()));

        if (submitResult.success()) {
            return "redirect:" + query.toUri(
                    appProperties.getUi().getHomePath() + "/cronjobs",
                    CRONJOB_PAGE_DEFAULTS);
        }

        model.addAttribute(
                CronJobListPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        query,
                        form,
                        submitResult.fieldErrors(),
                        "Please correct the form and try again.",
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
        var cronJobPage = cronJobService.getManyCronJobs(query.toPageable(CRONJOB_PAGE_DEFAULTS));
        List<CronJobTableRowView> rows = cronJobPage.getContent().stream()
                .map(result -> mapper.map(result, CronJobTableRowView.class))
                .toList();

        UiPaginationView pagination = uiPaginationFactory.build(
                cronJobPage,
                uiPaginationPathBuilder.build(request, query, CRONJOB_PAGE_DEFAULTS));

        UiTableView cronJobTable = uiTableFactory.build(
                UiTableDefinition.builder()
                        .title("Cronjob List")
                        .description("Review recurring job configs stored in the database.")
                        .emptyMessage("No cronjobs found.")
                        .pagination(pagination)
                        .build(),
                rows,
                CronJobTableRowView.class,
                row -> List.of(
                        UiTableActionView.builder()
                                .label("Metadata")
                                .path(buildMetadataPath(row.getJobType(), query))
                                .buttonClass("btn-outline-secondary")
                                .build(),
                        UiTableActionView.builder()
                                .label("Detail")
                                .path(buildDetailPath(row.getJobType(), query))
                                .buttonClass("btn-primary")
                                .build()));

        UiMetadataModalView metadataModal = metadataJobType == null
                ? null
                : buildMetadataModal(metadataJobType);

        UiModalView detailModal = detailJobType == null
                ? null
                : buildDetailModal(detailJobType, query, form, modalErrors, preserveDetailForm);

        return CronJobListPageView.builder()
                .title("Cronjob Management")
                .heading("Cronjobs")
                .description("Review recurring jobs and update cron overrides when operations need a schedule change.")
                .shell(buildShell(currentUser, request))
                .cronJobTable(cronJobTable)
                .metadataModal(metadataModal)
                .detailModal(detailModal)
                .errorMessage(errorMessage)
                .openMetadataModal(openMetadataModal && metadataModal != null)
                .openDetailModal(openDetailModal && detailModal != null)
                .build();
    }

    private UiMetadataModalView buildMetadataModal(String jobType) {
        CronJobDetailResult cronJob = cronJobService.getCronJobDetail(jobType);

        return UiMetadataModalView.builder()
                .id("cronjob-metadata-modal")
                .title("Cronjob Metadata")
                .items(List.of(
                        item("Id", String.valueOf(cronJob.getId()), true),
                        item("Job Type", cronJob.getJobType(), true),
                        item(
                                "Cron Override",
                                cronJob.isUsingDefaultCron()
                                        ? "Using default cron"
                                        : cronJob.getCronExpression(),
                                true),
                        item("Status", String.valueOf(cronJob.getStatus()), false),
                        item("Created At", cronJob.getCreatedAt(), true),
                        item("Updated At", cronJob.getUpdatedAt(), true)))
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
                        .title("Cronjob Detail")
                        .description("Review job metadata and update the cron override or status.")
                        .actionPath(query.toUri(
                                appProperties.getUi().getHomePath() + "/cronjobs/" + jobType,
                                CRONJOB_PAGE_DEFAULTS))
                        .submitLabel("Save Changes")
                        .build(),
                CronJobDetailModalForm.class,
                modalForm,
                Map.of("status", buildStatusOptions(modalForm.getStatus())),
                modalErrors == null ? Map.of() : modalErrors);
    }

    private CronJobDetailModalForm buildDetailForm(String jobType) {
        CronJobDetailResult cronJob = cronJobService.getCronJobDetail(jobType);

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

    private List<UiModalFieldOptionView> buildStatusOptions(CronjobStatusEnum selectedStatus) {
        return Arrays.stream(CronjobStatusEnum.values())
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

    private UiShellView buildShell(UserPrincipal currentUser, HttpServletRequest request) {
        return UiShellView.builder()
                .title(appProperties.getUi().getApplicationTitle())
                .logoutPath(appProperties.getUi().getLogoutPath())
                .currentUser(UiCurrentUserView.builder()
                        .email(currentUser.getEmail())
                        .authorities(currentUser.getAuthorities().stream()
                                .map(authority -> authority.getAuthority())
                                .toList())
                        .build())
                .menuTree(menuService.getMenuTree(request.getRequestURI()))
                .build();
    }
}
