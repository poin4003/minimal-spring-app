package com.app.features.user.web.controller;

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
import com.app.features.rbac.schema.filter.RoleFilterCriteria;
import com.app.features.rbac.schema.result.RoleResult;
import com.app.features.rbac.service.RbacService;
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
import com.app.features.user.schema.result.UserDetailResult;
import com.app.features.user.service.UserService;
import com.app.features.user.web.view.UserRolePageView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/users/{userId}/roles")
public class UserRolePageController {

    private static final UiPageDefaults USER_ROLE_PAGE_DEFAULTS = UiPageDefaults.builder()
            .page(0)
            .size(10)
            .sortBy("key")
            .sortDirection(Sort.Direction.ASC)
            .build();

    private final AppProperties appProperties;
    private final MenuService menuService;
    private final UserService userService;
    private final RbacService rbacService;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;

    @GetMapping
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @PathVariable UUID userId,
            @Valid @ModelAttribute("query") UiAssignmentPageQuery query,
            Model model) {
        model.addAttribute(
                UserRolePageView.ATTRIBUTE,
                buildPage(currentUser, request, userId, query, null));
        return "user/role/index";
    }

    @PostMapping("/assign")
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String assign(
            @PathVariable UUID userId,
            @RequestParam UUID targetId,
            @Valid @ModelAttribute("query") UiAssignmentPageQuery query) {
        rbacService.assignRoleToUser(userId, List.of(targetId));
        return "redirect:" + buildRedirectPath(userId, query);
    }

    @PostMapping("/remove")
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String remove(
            @PathVariable UUID userId,
            @RequestParam UUID targetId,
            @Valid @ModelAttribute("query") UiAssignmentPageQuery query) {
        rbacService.removeRoleFromUser(userId, List.of(targetId));
        return "redirect:" + buildRedirectPath(userId, query);
    }

    private UserRolePageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            UUID userId,
            UiAssignmentPageQuery query,
            String errorMessage) {
        UiAssignmentPageQuery resolvedQuery = query.applyDefaults(USER_ROLE_PAGE_DEFAULTS);
        UserDetailResult user = userService.getUserDetailById(userId);
        boolean assignedMode = resolvedQuery.getMode() == UiAssignmentMode.ASSIGNED;

        RoleFilterCriteria criteria = assignedMode
                ? buildAssignedCriteria(userId)
                : buildAvailableCriteria(userId);

        var rolePage = rbacService.getManyRoles(criteria, resolvedQuery.toPageable(USER_ROLE_PAGE_DEFAULTS));

        UiPaginationView pagination = uiPaginationFactory.build(
                rolePage,
                uiPaginationPathBuilder.build(request, resolvedQuery, USER_ROLE_PAGE_DEFAULTS));

        UiAssignmentPanelView assignmentPanel = UiAssignmentPanelView.builder()
                .title(assignedMode ? "Assigned Roles" : "Available Roles")
                .description(assignedMode
                        ? "Roles currently assigned to this user."
                        : "Roles that can be assigned to this user.")
                .emptyMessage(assignedMode
                        ? "No roles assigned."
                        : "No roles available.")
                .rows(rolePage.getContent().stream()
                        .map(role -> this.toPanelItem(userId, resolvedQuery, assignedMode, role))
                        .toList())
                .pagination(pagination)
                .build();

        return UserRolePageView.builder()
                .title("User Roles")
                .heading("User Roles")
                .description("Manage roles for the selected user.")
                .metadataItems(List.of(
                        UiMetadataItemView.builder()
                                .label("Email")
                                .value(user.getEmail())
                                .monospace(false)
                                .build(),
                        UiMetadataItemView.builder()
                                .label("Status")
                                .value(String.valueOf(user.getStatus()))
                                .monospace(false)
                                .build()))
                .shell(buildShell(currentUser, request))
                .backPath(appProperties.getUi().getHomePath() + "/users")
                .assignedPath(buildModePath(userId, resolvedQuery, UiAssignmentMode.ASSIGNED))
                .availablePath(buildModePath(userId, resolvedQuery, UiAssignmentMode.AVAILABLE))
                .assignedMode(assignedMode)
                .assignmentPanel(assignmentPanel)
                .errorMessage(errorMessage)
                .build();
    }

    private RoleFilterCriteria buildAssignedCriteria(UUID userId) {
        RoleFilterCriteria criteria = new RoleFilterCriteria();
        criteria.setUserId(userId);
        return criteria;
    }

    private RoleFilterCriteria buildAvailableCriteria(UUID userId) {
        RoleFilterCriteria criteria = new RoleFilterCriteria();
        criteria.setExcludeUserId(userId);
        return criteria;
    }

    private UiAssignmentPanelItemView toPanelItem(
            UUID userId,
            UiAssignmentPageQuery query,
            boolean assignedMode,
            RoleResult role) {
        return UiAssignmentPanelItemView.builder()
                .title(role.getKey())
                .description(role.getName())
                .action(UiAssignmentActionView.builder()
                        .path(assignedMode
                                ? appProperties.getUi().getHomePath() + "/users/" + userId + "/roles/remove"
                                : appProperties.getUi().getHomePath() + "/users/" + userId + "/roles/assign")
                        .label(assignedMode ? "Remove" : "Assign")
                        .buttonClass(assignedMode ? "btn-outline-danger" : "btn-outline-primary")
                        .targetId(role.getId().toString())
                        .query(query)
                        .build())
                .build();
    }

    private String buildRedirectPath(UUID userId, UiAssignmentPageQuery query) {
        return query.toUri(
                appProperties.getUi().getHomePath() + "/users/" + userId + "/roles",
                USER_ROLE_PAGE_DEFAULTS);
    }

    private String buildModePath(UUID userId, UiAssignmentPageQuery query, UiAssignmentMode mode) {
        return query.forMode(mode).toUri(
                appProperties.getUi().getHomePath() + "/users/" + userId + "/roles",
                USER_ROLE_PAGE_DEFAULTS);
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
