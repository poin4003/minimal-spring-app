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
import com.app.config.security.web.HtmxRequestSupport;
import com.app.core.constant.PermissionConstants;
import com.app.core.i18n.AppMessageResolver;
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
import com.app.features.ui.web.component.view.UiBreadcrumbItemView;
import com.app.features.ui.web.component.view.UiBreadcrumbView;
import com.app.features.ui.web.component.view.UiHtmxNavigationView;
import com.app.features.ui.web.component.view.UiMetadataItemView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.enums.UiAssignmentMode;
import com.app.features.ui.web.query.UiAssignmentPageQuery;
import com.app.features.ui.web.support.UiShellFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/rbac/roles/{roleId}/permissions")
public class RolePermissionPageController {

    private static final String ROLE_PERMISSION_PANEL_ID = "role-permission-assignment-panel";

    private static final UiPageDefaults ROLE_PERMISSION_PAGE_DEFAULTS = UiPageDefaults.builder()
            .page(0)
            .size(10)
            .sortBy("key")
            .sortDirection(Sort.Direction.ASC)
            .build();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final UiShellFactory uiShellFactory;
    private final RbacService rbacSvc;
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
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable UUID roleId,
            @RequestParam UUID targetId,
            @Valid @ModelAttribute("query") UiAssignmentPageQuery query) {
        rbacSvc.assignPermToRole(roleId, List.of(targetId));
        return HtmxRequestSupport.redirectView(
                request,
                response,
                buildRedirectPath(roleId, query));
    }

    @PostMapping("/remove")
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String remove(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable UUID roleId,
            @RequestParam UUID targetId,
            @Valid @ModelAttribute("query") UiAssignmentPageQuery query) {
        rbacSvc.removePermFromRole(roleId, List.of(targetId));
        return HtmxRequestSupport.redirectView(
                request,
                response,
                buildRedirectPath(roleId, query));
    }

    private RolePermissionPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            UUID roleId,
            UiAssignmentPageQuery query,
            String errorMessage) {
        UiAssignmentPageQuery resolvedQuery = query.applyDefaults(ROLE_PERMISSION_PAGE_DEFAULTS);
        RoleResult role = rbacSvc.getRole(roleId);
        boolean assignedMode = resolvedQuery.getMode() == UiAssignmentMode.ASSIGNED;

        PermissionFilterCriteria criteria = assignedMode
                ? buildAssignedCriteria(roleId)
                : buildAvailableCriteria(roleId);

        var permissionPage = rbacSvc.getManyPermissions(
                criteria,
                resolvedQuery.toPageable(ROLE_PERMISSION_PAGE_DEFAULTS));

        UiPaginationView pagination = uiPaginationFactory.build(
                permissionPage,
                uiPaginationPathBuilder.build(request, resolvedQuery, ROLE_PERMISSION_PAGE_DEFAULTS),
                UiHtmxNavigationView.forComponent(ROLE_PERMISSION_PANEL_ID));

        UiAssignmentPanelView assignmentPanel = UiAssignmentPanelView.builder()
                .id(ROLE_PERMISSION_PANEL_ID)
                .title(messageResolver.get(assignedMode
                        ? "rbac.rolePermission.assigned.title"
                        : "rbac.rolePermission.available.title"))
                .description(assignedMode
                        ? messageResolver.get("rbac.rolePermission.assigned.description")
                        : messageResolver.get("rbac.rolePermission.available.description"))
                .emptyMessage(assignedMode
                        ? messageResolver.get("rbac.rolePermission.assigned.empty")
                        : messageResolver.get("rbac.rolePermission.available.empty"))
                .rows(permissionPage.getContent().stream()
                        .map(permission -> this.toPanelItem(roleId, resolvedQuery, assignedMode, permission))
                        .toList())
                .pagination(pagination)
                .build();

        return RolePermissionPageView.builder()
                .title(messageResolver.get("rbac.rolePermission.page.title"))
                .heading(messageResolver.get("rbac.rolePermission.page.title"))
                .description(messageResolver.get("rbac.rolePermission.page.description"))
                .breadcrumb(UiBreadcrumbView.builder()
                        .items(List.of(
                                UiBreadcrumbItemView.builder()
                                        .label(messageResolver.get("menu.roles"))
                                        .path(appProperties.getUi().getHomePath() + "/rbac/roles")
                                        .build(),
                                UiBreadcrumbItemView.builder()
                                        .label(messageResolver.get("rbac.rolePermission.assign"))
                                        .active(true)
                                        .build()))
                        .build())
                .metadataItems(List.of(
                        UiMetadataItemView.builder()
                                .label(messageResolver.get("field.roleKey"))
                                .value(role.getKey())
                                .monospace(true)
                                .build(),
                        UiMetadataItemView.builder()
                                .label(messageResolver.get("field.roleName"))
                                .value(role.getName())
                                .monospace(false)
                                .build()))
                .shell(uiShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
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
                        .label(messageResolver.get(assignedMode
                                ? "action.remove"
                                : "action.assign"))
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

}
