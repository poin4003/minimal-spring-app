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

import com.app.config.settings.AppProperties;
import com.app.core.constant.PermissionConstants;
import com.app.core.menu.MenuService;
import com.app.core.security.UserPrincipal;
import com.app.features.rbac.schema.filter.PermissionFilterCriteria;
import com.app.features.rbac.schema.result.PermissionResult;
import com.app.features.rbac.schema.result.RoleResult;
import com.app.features.rbac.service.RbacService;
import com.app.features.rbac.web.view.PermissionTableRowView;
import com.app.features.rbac.web.view.RolePermissionActionForm;
import com.app.features.rbac.web.view.RolePermissionFilter;
import com.app.features.rbac.web.view.RolePermissionPageView;
import com.app.features.ui.web.component.support.UiModalFactory;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.support.UiTableFactory;
import com.app.features.ui.web.component.view.UiModalDefinition;
import com.app.features.ui.web.component.view.UiModalFieldOptionView;
import com.app.features.ui.web.component.view.UiModalView;
import com.app.features.ui.web.component.view.UiPaginationView;
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
@RequestMapping("${app.ui.home-path:/admin}/rbac/roles/{roleId}/permissions")
public class RolePermissionPageController {

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
            @PathVariable UUID roleId,
            @Valid @ModelAttribute("filter") RolePermissionFilter filter,
            Model model) {
        model.addAttribute(
                RolePermissionPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        roleId,
                        filter,
                        new RolePermissionActionForm(),
                        Map.of(),
                        new RolePermissionActionForm(),
                        Map.of(),
                        null,
                        false,
                        false));
        return "rbac/role-permission/index";
    }

    @PostMapping("/assign")
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String assign(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @PathVariable UUID roleId,
            @Valid @ModelAttribute("filter") RolePermissionFilter filter,
            @Valid @ModelAttribute("assignForm") RolePermissionActionForm assignForm,
            BindingResult bindingResult,
            Model model) {
        UiFormSubmitResult submitResult = uiFormSubmitSupport.submit(
                bindingResult,
                () -> rbacService.assignPermToRole(roleId, List.of(assignForm.getPermissionId())));

        if (submitResult.success()) {
            return "redirect:" + appProperties.getUi().getHomePath() + "/rbac/roles/" + roleId + "/permissions";
        }

        model.addAttribute(
                RolePermissionPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        roleId,
                        filter,
                        assignForm,
                        submitResult.fieldErrors(),
                        new RolePermissionActionForm(),
                        Map.of(),
                        "Please correct the form and try again.",
                        true,
                        false));
        return "rbac/role-permission/index";
    }

    @PostMapping("/remove")
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String remove(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @PathVariable UUID roleId,
            @Valid @ModelAttribute("filter") RolePermissionFilter filter,
            @Valid @ModelAttribute("removeForm") RolePermissionActionForm removeForm,
            BindingResult bindingResult,
            Model model) {
        UiFormSubmitResult submitResult = uiFormSubmitSupport.submit(
                bindingResult,
                () -> rbacService.removePermFromRole(roleId, List.of(removeForm.getPermissionId())));

        if (submitResult.success()) {
            return "redirect:" + appProperties.getUi().getHomePath() + "/rbac/roles/" + roleId + "/permissions";
        }

        model.addAttribute(
                RolePermissionPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        roleId,
                        filter,
                        new RolePermissionActionForm(),
                        Map.of(),
                        removeForm,
                        submitResult.fieldErrors(),
                        "Please correct the form and try again.",
                        false,
                        true));
        return "rbac/role-permission/index";
    }

    private RolePermissionPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            UUID roleId,
            RolePermissionFilter filter,
            RolePermissionActionForm assignForm,
            Map<String, String> assignErrors,
            RolePermissionActionForm removeForm,
            Map<String, String> removeErrors,
            String errorMessage,
            boolean openAssignPermissionModal,
            boolean openRemovePermissionModal) {
        RoleResult role = rbacService.getRole(roleId);

        PermissionFilterCriteria assignedCriteria = new PermissionFilterCriteria();
        assignedCriteria.setRoleId(roleId);

        var assignedPermissionPage = rbacService.getManyPermissions(assignedCriteria, filter.toPageable());
        List<PermissionTableRowView> assignedRows = assignedPermissionPage.getContent().stream()
                .map((PermissionResult permission) -> this.toRowView(permission))
                .toList();

        List<PermissionResult> assignedPermissions = rbacService.getManyPermissions(assignedCriteria, Pageable.unpaged())
                .getContent();
        Set<UUID> assignedPermissionIds = assignedPermissions.stream()
                .map((PermissionResult permission) -> permission.getId())
                .collect(Collectors.toSet());

        List<PermissionResult> allPermissions = rbacService.getManyPermissions(new PermissionFilterCriteria(), Pageable.unpaged())
                .getContent()
                .stream()
                .sorted(Comparator.comparing(
                        (PermissionResult permission) -> permission.getKey(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        UiPaginationView pagination = uiPaginationFactory.build(
                assignedPermissionPage,
                uiPaginationPathBuilder.build(request, filter));

        UiTableView assignedPermissionTable = uiTableFactory.build(
                UiTableDefinition.builder()
                        .title("Assigned Permissions")
                        .description("Review permissions currently granted to this role.")
                        .emptyMessage("No permissions assigned.")
                        .pagination(pagination)
                        .build(),
                assignedRows,
                PermissionTableRowView.class);

        UiModalView assignPermissionModal = uiModalFactory.build(
                UiModalDefinition.builder()
                        .id("assign-permission-modal")
                        .title("Assign Permission")
                        .description("Add one permission to this role.")
                        .triggerLabel("Assign Permission")
                        .triggerButtonClass("btn-primary")
                        .actionPath(appProperties.getUi().getHomePath() + "/rbac/roles/" + roleId + "/permissions/assign")
                        .submitLabel("Assign")
                        .build(),
                RolePermissionActionForm.class,
                assignForm,
                Map.of("permissionId", allPermissions.stream()
                        .filter(permission -> !assignedPermissionIds.contains(permission.getId()))
                        .map((PermissionResult permission) -> this.toOption(permission))
                        .toList()),
                assignErrors);

        UiModalView removePermissionModal = uiModalFactory.build(
                UiModalDefinition.builder()
                        .id("remove-permission-modal")
                        .title("Remove Permission")
                        .description("Remove one permission from this role.")
                        .triggerLabel("Remove Permission")
                        .triggerButtonClass("btn-outline-danger")
                        .actionPath(appProperties.getUi().getHomePath() + "/rbac/roles/" + roleId + "/permissions/remove")
                        .submitLabel("Remove")
                        .build(),
                RolePermissionActionForm.class,
                removeForm,
                Map.of("permissionId", assignedPermissions.stream()
                        .sorted(Comparator.comparing(
                                (PermissionResult permission) -> permission.getKey(),
                                String.CASE_INSENSITIVE_ORDER))
                        .map((PermissionResult permission) -> this.toOption(permission))
                        .toList()),
                removeErrors);

        return RolePermissionPageView.builder()
                .title("Role Permissions")
                .heading("Role Permissions")
                .description("Assign and remove permissions for the selected role.")
                .roleKey(role.getKey())
                .roleName(role.getName())
                .shell(buildShell(currentUser, request))
                .assignedPermissionTable(assignedPermissionTable)
                .assignPermissionModal(assignPermissionModal)
                .removePermissionModal(removePermissionModal)
                .errorMessage(errorMessage)
                .openAssignPermissionModal(openAssignPermissionModal)
                .openRemovePermissionModal(openRemovePermissionModal)
                .build();
    }

    private PermissionTableRowView toRowView(PermissionResult result) {
        return PermissionTableRowView.builder()
                .id(result.getId())
                .key(result.getKey())
                .name(result.getName())
                .build();
    }

    private UiModalFieldOptionView toOption(PermissionResult result) {
        return UiModalFieldOptionView.builder()
                .value(result.getId().toString())
                .label(result.getKey() + " - " + result.getName())
                .selected(false)
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
