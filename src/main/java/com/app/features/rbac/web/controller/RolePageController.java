package com.app.features.rbac.web.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.config.settings.AppProperties;
import com.app.core.constant.PermissionConstants;
import com.app.core.menu.MenuService;
import com.app.core.security.UserPrincipal;
import com.app.features.rbac.schema.filter.PermissionFilterCriteria;
import com.app.features.rbac.schema.filter.RoleFilterCriteria;
import com.app.features.rbac.schema.payload.CreateRolePayload;
import com.app.features.rbac.schema.result.PermissionResult;
import com.app.features.rbac.schema.result.RoleResult;
import com.app.features.rbac.service.RbacService;
import com.app.features.rbac.web.view.CreateRoleModalForm;
import com.app.features.rbac.web.view.RoleFilter;
import com.app.features.rbac.web.view.RoleListPageView;
import com.app.features.rbac.web.view.RoleTableRowView;
import com.app.features.ui.web.component.support.UiModalFactory;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.support.UiTableFactory;
import com.app.features.ui.web.component.view.UiDetailItemView;
import com.app.features.ui.web.component.view.UiDetailModalView;
import com.app.features.ui.web.component.view.UiMetadataItemView;
import com.app.features.ui.web.component.view.UiMetadataModalView;
import com.app.features.ui.web.component.view.UiModalDefinition;
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
@RequestMapping("${app.ui.home-path:/admin}/rbac/roles")
public class RolePageController {

    private final AppProperties appProperties;
    private final MenuService menuService;
    private final RbacService rbacService;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;
    private final UiTableFactory uiTableFactory;
    private final UiModalFactory uiModalFactory;
    private final UiFormSubmitSupport uiFormSubmitSupport;

    @GetMapping
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter") RoleFilter filter,
            @RequestParam(required = false) UUID metadataRoleId,
            @RequestParam(required = false) UUID detailRoleId,
            Model model) {
        model.addAttribute(
                RoleListPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        filter,
                        new CreateRoleModalForm(),
                        null,
                        null,
                        false,
                        metadataRoleId,
                        metadataRoleId != null,
                        detailRoleId,
                        detailRoleId != null));
        return "rbac/role/index";
    }

    @PostMapping
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String create(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter") RoleFilter filter,
            @Valid @ModelAttribute("form") CreateRoleModalForm form,
            BindingResult bindingResult,
            Model model) {
        UiFormSubmitResult submitResult = uiFormSubmitSupport.submit(
                bindingResult,
                () -> rbacService.createRole(toCreateRolePayload(form)));

        if (submitResult.success()) {
            return "redirect:" + appProperties.getUi().getHomePath() + "/rbac/roles";
        }

        model.addAttribute(
                RoleListPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        filter,
                        form,
                        submitResult.fieldErrors(),
                        "Please correct the form and try again.",
                        true,
                        null,
                        false,
                        null,
                        false));
        return "rbac/role/index";
    }

    private RoleListPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            RoleFilter filter,
            CreateRoleModalForm form,
            Map<String, String> modalErrors,
            String errorMessage,
            boolean openCreateRoleModal,
            UUID metadataRoleId,
            boolean openMetadataModal,
            UUID detailRoleId,
            boolean openDetailModal) {
        RoleFilterCriteria criteria = new RoleFilterCriteria();
        criteria.setUserId(filter.getUserId());

        var rolePage = rbacService.getManyRoles(criteria, filter.toPageable());
        List<RoleTableRowView> rows = rolePage.getContent().stream()
                .map(role -> this.toRowView(role))
                .toList();

        UiPaginationView pagination = uiPaginationFactory.build(
                rolePage,
                uiPaginationPathBuilder.build(request, filter));

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
                                .path(appProperties.getUi().getHomePath() + "/rbac/roles?metadataRoleId=" + row.getId())
                                .buttonClass("btn-outline-secondary")
                                .build(),
                        UiTableActionView.builder()
                                .label("Detail")
                                .path(appProperties.getUi().getHomePath() + "/rbac/roles?detailRoleId=" + row.getId())
                                .buttonClass("btn-outline-primary")
                                .build(),
                        UiTableActionView.builder()
                                .label("Manage Permissions")
                                .path(appProperties.getUi().getHomePath() + "/rbac/roles/" + row.getId() + "/permissions?mode=ASSIGNED")
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
                form,
                Map.of(),
                modalErrors == null ? Map.of() : modalErrors);

        UiMetadataModalView metadataModal = metadataRoleId == null
                ? null
                : buildRoleMetadataModal(metadataRoleId);

        UiDetailModalView detailModal = detailRoleId == null
                ? null
                : buildRoleDetailModal(detailRoleId);

        return RoleListPageView.builder()
                .title("Role Management")
                .heading("Roles")
                .description("Create roles and review access groups available in the system.")
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
        RoleResult role = rbacService.getRole(roleId);

        return UiMetadataModalView.builder()
                .id("role-metadata-modal")
                .title("Role Metadata")
                .items(List.of(
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

    private UiDetailModalView buildRoleDetailModal(UUID roleId) {
        PermissionFilterCriteria criteria = new PermissionFilterCriteria();
        criteria.setRoleId(roleId);

        List<UiDetailItemView> items = rbacService.getManyPermissions(criteria, Pageable.unpaged())
                .getContent()
                .stream()
                .sorted(Comparator.comparing(
                        (PermissionResult permission) -> permission.getKey(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(permission -> UiDetailItemView.builder()
                        .title(permission.getKey())
                        .description(permission.getName())
                        .build())
                .toList();

        return UiDetailModalView.builder()
                .id("role-detail-modal")
                .title("Role Detail")
                .listTitle("Assigned Permissions")
                .items(items)
                .emptyMessage("No permissions assigned.")
                .build();
    }

    private CreateRolePayload toCreateRolePayload(CreateRoleModalForm form) {
        CreateRolePayload payload = new CreateRolePayload();
        payload.setName(form.getName());
        payload.setKey(form.getKey());
        return payload;
    }

    private RoleTableRowView toRowView(RoleResult result) {
        return RoleTableRowView.builder()
                .id(result.getId())
                .key(result.getKey())
                .name(result.getName())
                .build();
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
