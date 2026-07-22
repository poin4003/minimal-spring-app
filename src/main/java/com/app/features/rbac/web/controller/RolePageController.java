package com.app.features.rbac.web.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.app.core.constant.PermissionConstants;
import com.app.core.menu.MenuService;
import com.app.core.security.UserPrincipal;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.features.rbac.schema.filter.RoleFilterCriteria;
import com.app.features.rbac.schema.payload.CreateRolePayload;
import com.app.features.rbac.schema.payload.UpdateRolePayload;
import com.app.features.rbac.schema.result.RoleResult;
import com.app.features.rbac.service.RbacService;
import com.app.features.rbac.web.view.CreateRoleModalForm;
import com.app.features.rbac.web.view.RoleDetailModalForm;
import com.app.features.rbac.web.view.RoleFilter;
import com.app.features.rbac.web.view.RoleListPageView;
import com.app.features.rbac.web.view.RoleTableRowView;
import com.app.features.ui.web.component.support.UiModalFactory;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.support.UiTableFactory;
import com.app.features.ui.web.component.view.UiMetadataItemView;
import com.app.features.ui.web.component.view.UiMetadataModalView;
import com.app.features.ui.web.component.view.UiModalDefinition;
import com.app.features.ui.web.component.view.UiModalView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.component.view.UiTableActionView;
import com.app.features.ui.web.component.view.UiTableDefinition;
import com.app.features.ui.web.component.view.UiTableView;
import com.app.features.ui.web.enums.UiAssignmentMode;
import com.app.features.ui.web.query.UiAssignmentPageQuery;
import com.app.features.ui.web.support.UiFormSubmitResult;
import com.app.features.ui.web.support.UiFormSubmitSupport;
import com.app.features.ui.web.view.UiCurrentUserView;
import com.app.features.ui.web.view.UiShellView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/rbac/roles")
public class RolePageController {

    private static final UiPageDefaults ROLE_PAGE_DEFAULTS = UiPageDefaults.builder()
            .page(0)
            .size(10)
            .sortBy("key")
            .sortDirection(Sort.Direction.ASC)
            .build();

    private static final UiPageDefaults ROLE_ASSIGNMENT_PAGE_DEFAULTS = UiPageDefaults.builder()
            .page(0)
            .size(10)
            .sortBy("key")
            .sortDirection(Sort.Direction.ASC)
            .build();

    private final AppProperties appProperties;
    private final MenuService menuSvc;
    private final RbacService rbacSvc;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;
    private final UiTableFactory uiTableFactory;
    private final UiModalFactory uiModalFactory;
    private final UiFormSubmitSupport uiFormSubmitSupport;
    private final ModelMapper mapper;

    @GetMapping
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter") RoleFilter filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @RequestParam(required = false) UUID metadataRoleId,
            @RequestParam(required = false) UUID detailRoleId,
            Model model) {
        model.addAttribute(
                RoleListPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        filter,
                        query,
                        new CreateRoleModalForm(),
                        new RoleDetailModalForm(),
                        null,
                        null,
                        false,
                        metadataRoleId,
                        metadataRoleId != null,
                        detailRoleId,
                        detailRoleId != null,
                        false));
        return "rbac/role/index";
    }

    @PostMapping
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String create(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter") RoleFilter filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @Valid @ModelAttribute("createForm") CreateRoleModalForm form,
            BindingResult bindingResult,
            Model model) {
        UiFormSubmitResult submitResult = uiFormSubmitSupport.submit(
                bindingResult,
                () -> rbacSvc.createRole(mapper.map(form, CreateRolePayload.class)));

        if (submitResult.success()) {
            return "redirect:" + appProperties.getUi().getHomePath() + "/rbac/roles";
        }

        model.addAttribute(
                RoleListPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        filter,
                        query,
                        form,
                        new RoleDetailModalForm(),
                        submitResult.fieldErrors(),
                        "Please correct the form and try again.",
                        true,
                        null,
                        false,
                        null,
                        false,
                        false));
        return "rbac/role/index";
    }

    @PostMapping("/{roleId}")
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String update(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @PathVariable UUID roleId,
            @Valid @ModelAttribute("filter") RoleFilter filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @Valid @ModelAttribute("detailForm") RoleDetailModalForm form,
            BindingResult bindingResult,
            Model model) {
        UiFormSubmitResult submitResult = uiFormSubmitSupport.submit(
                bindingResult,
                () -> rbacSvc.updateRole(roleId, mapper.map(form, UpdateRolePayload.class)));

        if (submitResult.success()) {
            return "redirect:" + query.toUri(
                    appProperties.getUi().getHomePath() + "/rbac/roles",
                    ROLE_PAGE_DEFAULTS);
        }

        model.addAttribute(
                RoleListPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        filter,
                        query,
                        new CreateRoleModalForm(),
                        form,
                        submitResult.fieldErrors(),
                        "Please correct the form and try again.",
                        false,
                        null,
                        false,
                        roleId,
                        true,
                        true));
        return "rbac/role/index";
    }

    private RoleListPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            RoleFilter filter,
            UiPageQuery query,
            CreateRoleModalForm createForm,
            RoleDetailModalForm detailForm,
            Map<String, String> modalErrors,
            String errorMessage,
            boolean openCreateRoleModal,
            UUID metadataRoleId,
            boolean openMetadataModal,
            UUID detailRoleId,
            boolean openDetailModal,
            boolean preserveDetailForm) {
        RoleFilterCriteria criteria = new RoleFilterCriteria();
        criteria.setUserId(filter.getUserId());

        var rolePage = rbacSvc.getManyRoles(criteria, query.toPageable(ROLE_PAGE_DEFAULTS));
        List<RoleTableRowView> rows = rolePage.getContent().stream()
                .map(role -> mapper.map(role, RoleTableRowView.class))
                .toList();

        UiPaginationView pagination = uiPaginationFactory.build(
                rolePage,
                uiPaginationPathBuilder.build(request, query, ROLE_PAGE_DEFAULTS));

        UiTableView roleTable = uiTableFactory.build(
                UiTableDefinition.builder()
                        .title("Role List")
                        .description("Review role keys and display names.")
                        .emptyMessage("No roles found.")
                        .pagination(pagination)
                        .build(),
                rows,
                RoleTableRowView.class,
                row -> List.of(
                        UiTableActionView.builder()
                                .label("Metadata")
                                .path(buildMetadataPath(row.getId(), query))
                                .buttonClass("btn-outline-secondary")
                                .build(),
                        UiTableActionView.builder()
                                .label("Detail")
                                .path(buildDetailPath(row.getId(), query))
                                .buttonClass("btn-outline-primary")
                                .build(),
                        UiTableActionView.builder()
                                .label("Manage Permissions")
                                .path(buildManagePermissionsPath(row.getId()))
                                .buttonClass("btn-primary")
                                .build()));

        UiModalView createRoleModal = uiModalFactory.build(
                UiModalDefinition.builder()
                        .id("create-role-modal")
                        .title("Create Role")
                        .description("Add a new role for access control.")
                        .triggerLabel("New Role")
                        .triggerButtonClass("btn-primary")
                        .actionPath(appProperties.getUi().getHomePath() + "/rbac/roles")
                        .submitLabel("Create Role")
                        .build(),
                CreateRoleModalForm.class,
                createForm,
                Map.of(),
                openCreateRoleModal && modalErrors != null ? modalErrors : Map.of());

        UiMetadataModalView metadataModal = metadataRoleId == null
                ? null
                : buildRoleMetadataModal(metadataRoleId);

        UiModalView detailModal = detailRoleId == null
                ? null
                : buildRoleDetailModal(detailRoleId, query, detailForm, modalErrors, preserveDetailForm);

        return RoleListPageView.builder()
                .title("Role Management")
                .shell(buildShell(currentUser, request))
                .roleTable(roleTable)
                .createRoleModal(createRoleModal)
                .metadataModal(metadataModal)
                .detailModal(detailModal)
                .errorMessage(errorMessage)
                .openCreateRoleModal(openCreateRoleModal)
                .openMetadataModal(openMetadataModal && metadataModal != null)
                .openDetailModal(openDetailModal && detailModal != null)
                .build();
    }

    private UiMetadataModalView buildRoleMetadataModal(UUID roleId) {
        RoleResult role = rbacSvc.getRole(roleId);

        return UiMetadataModalView.builder()
                .id("role-metadata-modal")
                .title("Role Metadata")
                .items(List.of(
                        UiMetadataItemView.builder()
                                .label("Role Id")
                                .value(role.getId())
                                .monospace(true)
                                .build(),
                        UiMetadataItemView.builder()
                                .label("Role Key")
                                .value(role.getKey())
                                .monospace(true)
                                .build(),
                        UiMetadataItemView.builder()
                                .label("Role Name")
                                .value(role.getName())
                                .monospace(false)
                                .build()))
                .build();
    }

    private UiModalView buildRoleDetailModal(
            UUID roleId,
            UiPageQuery query,
            RoleDetailModalForm detailForm,
            Map<String, String> modalErrors,
            boolean preserveDetailForm) {
        RoleResult role = rbacSvc.getRole(roleId);

        RoleDetailModalForm modalForm = preserveDetailForm
                ? detailForm
                : buildRoleDetailForm(role);

        return uiModalFactory.build(
                UiModalDefinition.builder()
                        .id("role-detail-modal")
                        .title("Role Detail")
                        .description("Review role information and update the role name or key.")
                        .actionPath(query.toUri(
                                appProperties.getUi().getHomePath() + "/rbac/roles/" + roleId,
                                ROLE_PAGE_DEFAULTS))
                        .submitLabel("Save Changes")
                        .build(),
                RoleDetailModalForm.class,
                modalForm,
                Map.of(),
                preserveDetailForm && modalErrors != null ? modalErrors : Map.of());
    }

    private RoleDetailModalForm buildRoleDetailForm(RoleResult role) {
        RoleDetailModalForm form = new RoleDetailModalForm();
        form.setName(role.getName());
        form.setKey(role.getKey());
        form.setCreatedAt(role.getCreatedAt());
        form.setUpdatedAt(role.getUpdatedAt());
        return form;
    }

    private String buildMetadataPath(String roleId, UiPageQuery query) {
        return UriComponentsBuilder.fromUriString(query.toUri(
                appProperties.getUi().getHomePath() + "/rbac/roles",
                ROLE_PAGE_DEFAULTS))
                .replaceQueryParam("detailRoleId")
                .replaceQueryParam("metadataRoleId", roleId)
                .build()
                .encode()
                .toUriString();
    }

    private String buildDetailPath(String roleId, UiPageQuery query) {
        return UriComponentsBuilder.fromUriString(query.toUri(
                appProperties.getUi().getHomePath() + "/rbac/roles",
                ROLE_PAGE_DEFAULTS))
                .replaceQueryParam("metadataRoleId")
                .replaceQueryParam("detailRoleId", roleId)
                .build()
                .encode()
                .toUriString();
    }

    private String buildManagePermissionsPath(String roleId) {
        UiAssignmentPageQuery query = new UiAssignmentPageQuery();
        query.setMode(UiAssignmentMode.ASSIGNED);
        return query.toUri(
                appProperties.getUi().getHomePath() + "/rbac/roles/" + roleId + "/permissions",
                ROLE_ASSIGNMENT_PAGE_DEFAULTS);
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
                .menuTree(menuSvc.getMenuTree(request.getRequestURI()))
                .build();
    }
}
