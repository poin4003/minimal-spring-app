package com.app.features.user.web.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.settings.AppProperties;
import com.app.core.constant.PermissionConstants;
import com.app.core.menu.MenuService;
import com.app.core.security.UserPrincipal;
import com.app.features.rbac.schema.filter.RoleFilterCriteria;
import com.app.features.rbac.schema.result.RoleResult;
import com.app.features.rbac.service.RbacService;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.view.UiAssignmentPanelItemView;
import com.app.features.ui.web.component.view.UiAssignmentPanelView;
import com.app.features.ui.web.component.view.UiMetadataItemView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.enums.UiAssignmentMode;
import com.app.features.ui.web.view.UiCurrentUserView;
import com.app.features.ui.web.view.UiShellView;
import com.app.features.user.schema.result.UserDetailResult;
import com.app.features.user.service.UserService;
import com.app.features.user.web.view.UserRoleFilter;
import com.app.features.user.web.view.UserRolePageView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/users/{userId}/roles")
public class UserRolePageController {

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
            @Valid @ModelAttribute("filter") UserRoleFilter filter,
            Model model) {
        model.addAttribute(
                UserRolePageView.ATTRIBUTE,
                buildPage(currentUser, request, userId, filter, null));
        return "user/role/index";
    }

    @PostMapping("/assign")
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String assign(
            @PathVariable UUID userId,
            @RequestParam UUID roleId,
            @Valid @ModelAttribute("filter") UserRoleFilter filter) {
        rbacService.assignRoleToUser(userId, List.of(roleId));
        return "redirect:" + buildRedirectPath(userId, filter);
    }

    @PostMapping("/remove")
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String remove(
            @PathVariable UUID userId,
            @RequestParam UUID roleId,
            @Valid @ModelAttribute("filter") UserRoleFilter filter) {
        rbacService.removeRoleFromUser(userId, List.of(roleId));
        return "redirect:" + buildRedirectPath(userId, filter);
    }

    private UserRolePageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            UUID userId,
            UserRoleFilter filter,
            String errorMessage) {
        UserDetailResult user = userService.getUserDetailById(userId);
        boolean assignedMode = filter.getMode() == UiAssignmentMode.ASSIGNED;

        RoleFilterCriteria criteria = assignedMode
                ? buildAssignedCriteria(userId)
                : buildAvailableCriteria(userId);

        var rolePage = rbacService.getManyRoles(criteria, filter.toPageable());

        UiPaginationView pagination = uiPaginationFactory.build(
                rolePage,
                uiPaginationPathBuilder.build(request, filter));

        UiAssignmentPanelView assignmentPanel = UiAssignmentPanelView.builder()
                .title(assignedMode ? "Assigned Roles" : "Available Roles")
                .description(assignedMode
                        ? "Roles currently assigned to this user."
                        : "Roles that can be assigned to this user.")
                .emptyMessage(assignedMode
                        ? "No roles assigned."
                        : "No roles available.")
                .rows(rolePage.getContent().stream()
                        .map(role -> this.toPanelItem(userId, filter, assignedMode, role))
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
                .assignedPath(buildModePath(userId, filter, UiAssignmentMode.ASSIGNED))
                .availablePath(buildModePath(userId, filter, UiAssignmentMode.AVAILABLE))
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
            UserRoleFilter filter,
            boolean assignedMode,
            RoleResult role) {
        return UiAssignmentPanelItemView.builder()
                .title(role.getKey())
                .description(role.getName())
                .actionPath(assignedMode
                        ? appProperties.getUi().getHomePath() + "/users/" + userId + "/roles/remove"
                        : appProperties.getUi().getHomePath() + "/users/" + userId + "/roles/assign")
                .actionLabel(assignedMode ? "Remove" : "Assign")
                .actionButtonClass(assignedMode ? "btn-outline-danger" : "btn-outline-primary")
                .hiddenFields(buildHiddenFields("roleId", role.getId(), filter))
                .build();
    }

    private Map<String, String> buildHiddenFields(
            String targetFieldName,
            String targetFieldValue,
            UserRoleFilter filter) {
        LinkedHashMap<String, String> hiddenFields = new LinkedHashMap<>();
        hiddenFields.put(targetFieldName, targetFieldValue);
        hiddenFields.put("mode", filter.getMode().name());
        hiddenFields.put("page", String.valueOf(filter.getPage()));
        hiddenFields.put("size", String.valueOf(filter.getSize()));
        hiddenFields.put("sortBy", filter.getSortBy());
        hiddenFields.put("sortDirection", filter.getSortDirection().name());
        return hiddenFields;
    }

    private String buildRedirectPath(UUID userId, UserRoleFilter filter) {
        return UriComponentsBuilder.fromPath(appProperties.getUi().getHomePath() + "/users/" + userId + "/roles")
                .queryParam("mode", filter.getMode().name())
                .queryParam("page", filter.getPage())
                .queryParam("size", filter.getSize())
                .queryParam("sortBy", filter.getSortBy())
                .queryParam("sortDirection", filter.getSortDirection().name())
                .build()
                .encode()
                .toUriString();
    }

    private String buildModePath(UUID userId, UserRoleFilter filter, UiAssignmentMode mode) {
        return UriComponentsBuilder.fromPath(appProperties.getUi().getHomePath() + "/users/" + userId + "/roles")
                .queryParam("mode", mode.name())
                .queryParam("size", filter.getSize())
                .queryParam("sortBy", filter.getSortBy())
                .queryParam("sortDirection", filter.getSortDirection().name())
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
