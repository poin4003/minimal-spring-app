package com.app.features.rbac.web.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
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
import com.app.features.ui.web.component.view.UiAssignmentDetailItemView;
import com.app.features.ui.web.component.view.UiAssignmentDetailMetaView;
import com.app.features.ui.web.component.view.UiAssignmentDetailModalView;
import com.app.features.rbac.web.view.RoleTableRowView;
import com.app.features.ui.web.component.support.UiModalFactory;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.support.UiTableFactory;
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
                        false));
        return "rbac/role/index";
    }

    @PostMapping("/{roleId}/detail/assign")
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String assignPermissionFromDetail(
            @PathVariable UUID roleId,
            @RequestParam UUID permissionId) {
        rbacService.assignPermToRole(roleId, List.of(permissionId));

        return "redirect:" + appProperties.getUi().getHomePath()
                + "/rbac/roles?detailRoleId=" + roleId;
    }

    @PostMapping("/{roleId}/detail/remove")
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String removePermissionFromDetail(
            @PathVariable UUID roleId,
            @RequestParam UUID permissionId) {
        rbacService.removePermFromRole(roleId, List.of(permissionId));

        return "redirect:" + appProperties.getUi().getHomePath()
                + "/rbac/roles?detailRoleId=" + roleId;
    }

    private RoleListPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            RoleFilter filter,
            CreateRoleModalForm form,
            Map<String, String> modalErrors,
            String errorMessage,
            boolean openCreateRoleModal,
            UUID detailRoleId,
            boolean openDetailModal) {
        RoleFilterCriteria criteria = new RoleFilterCriteria();
        criteria.setUserId(filter.getUserId());

        var rolePage = rbacService.getManyRoles(criteria, filter.toPageable());
        List<RoleTableRowView> rows = rolePage.getContent().stream()
                .map((RoleResult role) -> this.toRowView(role))
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
                                .label("Detail")
                                .path(appProperties.getUi().getHomePath() + "/rbac/roles?detailRoleId=" + row.getId())
                                .buttonClass("btn-outline-primary")
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

        UiAssignmentDetailModalView detailModal = detailRoleId == null
                ? null
                : buildRoleDetailModal(detailRoleId);

        return RoleListPageView.builder()
                .title("Role Management")
                .heading("Roles")
                .description("Create roles and review access groups available in the system.")
                .shell(buildShell(currentUser, request))
                .roleTable(roleTable)
                .createRoleModal(createRoleModal)
                .detailModal(detailModal)
                .errorMessage(errorMessage)
                .openCreateRoleModal(openCreateRoleModal)
                .openDetailModal(openDetailModal)
                .build();
    }

    private UiAssignmentDetailModalView buildRoleDetailModal(UUID roleId) {
        RoleResult role = rbacService.getRole(roleId);

        PermissionFilterCriteria assignedCriteria = new PermissionFilterCriteria();
        assignedCriteria.setRoleId(roleId);

        List<PermissionResult> assignedPermissions = rbacService.getManyPermissions(
                assignedCriteria,
                Pageable.unpaged())
                .getContent();

        Set<UUID> assignedPermissionIds = assignedPermissions.stream()
                .map((PermissionResult permission) -> permission.getId())
                .collect(Collectors.toSet());

        List<PermissionResult> allPermissions = rbacService.getManyPermissions(
                new PermissionFilterCriteria(),
                Pageable.unpaged())
                .getContent()
                .stream()
                .sorted(Comparator.comparing(
                        (PermissionResult permission) -> permission.getKey(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<UiAssignmentDetailItemView> assignedItems = assignedPermissions.stream()
                .sorted(Comparator.comparing(
                        (PermissionResult permission) -> permission.getKey(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(permission -> UiAssignmentDetailItemView.builder()
                        .title(permission.getKey())
                        .description(permission.getName())
                        .actionPath(appProperties.getUi().getHomePath() + "/rbac/roles/" + roleId + "/detail/remove")
                        .actionLabel("Remove")
                        .actionButtonClass("btn-outline-danger")
                        .hiddenFieldName("permissionId")
                        .hiddenFieldValue(permission.getId().toString())
                        .build())
                .toList();

        List<UiAssignmentDetailItemView> availableItems = allPermissions.stream()
                .filter(permission -> !assignedPermissionIds.contains(permission.getId()))
                .map(permission -> UiAssignmentDetailItemView.builder()
                        .title(permission.getKey())
                        .description(permission.getName())
                        .actionPath(appProperties.getUi().getHomePath() + "/rbac/roles/" + roleId + "/detail/assign")
                        .actionLabel("Assign")
                        .actionButtonClass("btn-outline-primary")
                        .hiddenFieldName("permissionId")
                        .hiddenFieldValue(permission.getId().toString())
                        .build())
                .toList();

        return UiAssignmentDetailModalView.builder()
                .id("role-detail-modal")
                .title("Role Detail")
                .metadata(List.of(
                        UiAssignmentDetailMetaView.builder()
                                .label("Role Key")
                                .value(role.getKey())
                                .monospace(true)
                                .build(),
                        UiAssignmentDetailMetaView.builder()
                                .label("Role Name")
                                .value(role.getName())
                                .monospace(false)
                                .build()))
                .assignedTitle("Assigned Permissions")
                .assignedEmptyMessage("No permissions assigned.")
                .assignedItems(assignedItems)
                .availableTitle("Available Permissions")
                .availableEmptyMessage("All permissions are already assigned.")
                .availableItems(availableItems)
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
