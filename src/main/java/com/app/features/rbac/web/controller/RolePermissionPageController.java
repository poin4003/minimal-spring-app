package com.app.features.rbac.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
import com.app.core.schema.query.UiPageDefaults;
import com.app.features.rbac.schema.filter.PermissionFilterCriteria;
import com.app.features.rbac.schema.result.PermissionResult;
import com.app.features.rbac.schema.result.RoleResult;
import com.app.features.rbac.service.RbacService;
import com.app.features.rbac.web.view.RolePermissionPageView;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.view.UiAssignmentActionView;
import com.app.features.ui.web.component.view.UiAssignmentPanelItemView;
import com.app.features.ui.web.component.view.UiAssignmentPanelView;
import com.app.features.ui.web.component.view.UiMetadataItemView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.enums.UiAssignmentMode;
import com.app.features.ui.web.query.UiAssignmentPageQuery;
import com.app.features.ui.web.view.UiCurrentUserView;
import com.app.features.ui.web.view.UiShellView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/rbac/roles/{roleId}/permissions")
public class RolePermissionPageController {

    private static final UiPageDefaults ROLE_PERMISSION_PAGE_DEFAULTS = UiPageDefaults.builder()
            .page(0)
            .size(10)
            .sortBy("key")
            .sortDirection(Sort.Direction.ASC)
            .build();

    private final AppProperties appProperties;
    private final MenuService menuService;
    private final RbacService rbacService;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;

    @GetMapping
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @PathVariable UUID roleId,
            @Valid @ModelAttribute("query") UiAssignmentPageQuery query,
            Model model) {
        model.addAttribute(
                RolePermissionPageView.ATTRIBUTE,
                buildPage(currentUser, request, roleId, query, null));
        return "rbac/role-permission/index";
    }

    @PostMapping("/assign")
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String assign(
            @PathVariable UUID roleId,
            @RequestParam UUID targetId,
            @Valid @ModelAttribute("query") UiAssignmentPageQuery query) {
        rbacService.assignPermToRole(roleId, List.of(targetId));
        return "redirect:" + buildRedirectPath(roleId, query);
    }

    @PostMapping("/remove")
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String remove(
            @PathVariable UUID roleId,
            @RequestParam UUID targetId,
            @Valid @ModelAttribute("query") UiAssignmentPageQuery query) {
        rbacService.removePermFromRole(roleId, List.of(targetId));
        return "redirect:" + buildRedirectPath(roleId, query);
    }

    private RolePermissionPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            UUID roleId,
            UiAssignmentPageQuery query,
            String errorMessage) {
        UiAssignmentPageQuery resolvedQuery = query.applyDefaults(ROLE_PERMISSION_PAGE_DEFAULTS);
        RoleResult role = rbacService.getRole(roleId);
        boolean assignedMode = resolvedQuery.getMode() == UiAssignmentMode.ASSIGNED;

        PermissionFilterCriteria criteria = assignedMode
                ? buildAssignedCriteria(roleId)
                : buildAvailableCriteria(roleId);

        var permissionPage = rbacService.getManyPermissions(
                criteria,
                resolvedQuery.toPageable(ROLE_PERMISSION_PAGE_DEFAULTS));

        UiPaginationView pagination = uiPaginationFactory.build(
                permissionPage,
                uiPaginationPathBuilder.build(request, resolvedQuery, ROLE_PERMISSION_PAGE_DEFAULTS));

        UiAssignmentPanelView assignmentPanel = UiAssignmentPanelView.builder()
                .title(assignedMode ? "Assigned Permissions" : "Available Permissions")
                .description(assignedMode
                        ? "Permissions currently granted to this role."
                        : "Permissions that can be assigned to this role.")
                .emptyMessage(assignedMode
                        ? "No permissions assigned."
                        : "No permissions available.")
                .rows(permissionPage.getContent().stream()
                        .map(permission -> this.toPanelItem(roleId, resolvedQuery, assignedMode, permission))
                        .toList())
                .pagination(pagination)
                .build();

        return RolePermissionPageView.builder()
                .title("Role Permissions")
                .heading("Role Permissions")
                .description("Manage permissions for the selected role.")
                .metadataItems(List.of(
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
                .shell(buildShell(currentUser, request))
                .backPath(appProperties.getUi().getHomePath() + "/rbac/roles")
                .assignedPath(buildModePath(roleId, resolvedQuery, UiAssignmentMode.ASSIGNED))
                .availablePath(buildModePath(roleId, resolvedQuery, UiAssignmentMode.AVAILABLE))
                .assignedMode(assignedMode)
                .assignmentPanel(assignmentPanel)
                .errorMessage(errorMessage)
                .build();
    }

    private PermissionFilterCriteria buildAssignedCriteria(UUID roleId) {
        PermissionFilterCriteria criteria = new PermissionFilterCriteria();
        criteria.setRoleId(roleId);
        return criteria;
    }

    private PermissionFilterCriteria buildAvailableCriteria(UUID roleId) {
        PermissionFilterCriteria criteria = new PermissionFilterCriteria();
        criteria.setExcludeRoleId(roleId);
        return criteria;
    }

    private UiAssignmentPanelItemView toPanelItem(
            UUID roleId,
            UiAssignmentPageQuery query,
            boolean assignedMode,
            PermissionResult permission) {
        return UiAssignmentPanelItemView.builder()
                .title(permission.getKey())
                .description(permission.getName())
                .action(UiAssignmentActionView.builder()
                        .path(assignedMode
                                ? appProperties.getUi().getHomePath() + "/rbac/roles/" + roleId + "/permissions/remove"
                                : appProperties.getUi().getHomePath() + "/rbac/roles/" + roleId + "/permissions/assign")
                        .label(assignedMode ? "Remove" : "Assign")
                        .buttonClass(assignedMode ? "btn-outline-danger" : "btn-outline-primary")
                        .targetId(permission.getId().toString())
                        .query(query)
                        .build())
                .build();
    }

    private String buildRedirectPath(UUID roleId, UiAssignmentPageQuery query) {
        return query.toUri(
                appProperties.getUi().getHomePath() + "/rbac/roles/" + roleId + "/permissions",
                ROLE_PERMISSION_PAGE_DEFAULTS);
    }

    private String buildModePath(UUID roleId, UiAssignmentPageQuery query, UiAssignmentMode mode) {
        return query.forMode(mode).toUri(
                appProperties.getUi().getHomePath() + "/rbac/roles/" + roleId + "/permissions",
                ROLE_PERMISSION_PAGE_DEFAULTS);
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
