package com.app.features.user.web.controller;

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
import com.app.features.rbac.schema.filter.RoleFilterCriteria;
import com.app.features.rbac.schema.result.RoleResult;
import com.app.features.rbac.service.RbacService;
import com.app.features.ui.web.component.support.UiModalFactory;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.support.UiTableFactory;
import com.app.features.ui.web.component.view.UiAssignmentDetailItemView;
import com.app.features.ui.web.component.view.UiAssignmentDetailMetaView;
import com.app.features.ui.web.component.view.UiAssignmentDetailModalView;
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
import com.app.features.user.schema.filter.UserFilter;
import com.app.features.user.schema.payload.CreateUserPayload;
import com.app.features.user.schema.result.UserDetailResult;
import com.app.features.user.schema.result.UserResult;
import com.app.features.user.service.UserService;
import com.app.features.user.web.view.CreateUserModalForm;
import com.app.features.user.web.view.UserListPageView;
import com.app.features.user.web.view.UserTableRowView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/users")
public class UserPageController {

    private final AppProperties appProperties;
    private final MenuService menuService;
    private final UserService userService;
    private final RbacService rbacService;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;
    private final UiTableFactory uiTableFactory;
    private final UiModalFactory uiModalFactory;
    private final UiFormSubmitSupport uiFormSubmitSupport;

    @GetMapping
    @Secured(PermissionConstants.USER_VIEW)
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter") UserFilter filter,
            @RequestParam(required = false) UUID detailUserId,
            Model model) {
        model.addAttribute(
                UserListPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        filter,
                        new CreateUserModalForm(),
                        null,
                        null,
                        false,
                        detailUserId,
                        detailUserId != null));
        return "user/index";
    }

    @PostMapping
    @Secured(PermissionConstants.USER_CREATE)
    public String create(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter") UserFilter filter,
            @Valid @ModelAttribute("form") CreateUserModalForm form,
            BindingResult bindingResult,
            Model model) {
        UiFormSubmitResult submitResult = uiFormSubmitSupport.submit(
                bindingResult,
                () -> userService.createUser(toCreateUserPayload(form)));

        if (submitResult.success()) {
            return "redirect:" + appProperties.getUi().getHomePath() + "/users";
        }

        model.addAttribute(
                UserListPageView.ATTRIBUTE,
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
        return "user/index";
    }

    @PostMapping("/{userId}/detail/assign-role")
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String assignRoleFromDetail(
            @PathVariable UUID userId,
            @RequestParam UUID roleId) {
        rbacService.assignRoleToUser(userId, List.of(roleId));

        return "redirect:" + appProperties.getUi().getHomePath()
                + "/users?detailUserId=" + userId;
    }

    @PostMapping("/{userId}/detail/remove-role")
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String removeRoleFromDetail(
            @PathVariable UUID userId,
            @RequestParam UUID roleId) {
        rbacService.removeRoleFromUser(userId, List.of(roleId));

        return "redirect:" + appProperties.getUi().getHomePath()
                + "/users?detailUserId=" + userId;
    }

    private UserListPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            UserFilter filter,
            CreateUserModalForm form,
            Map<String, String> modalErrors,
            String errorMessage,
            boolean openCreateUserModal,
            UUID detailUserId,
            boolean openDetailModal) {
        boolean canCreateUser = hasAuthority(currentUser, PermissionConstants.USER_CREATE);
        boolean canManageRbac = hasAuthority(currentUser, PermissionConstants.RBAC_MANAGE);

        var userPage = userService.getManyUser(filter.toPageable());
        List<UserTableRowView> rows = userPage.getContent().stream()
                .map(result -> this.toRowView(result))
                .toList();

        UiPaginationView pagination = uiPaginationFactory.build(
                userPage,
                uiPaginationPathBuilder.build(request, filter));

        UiTableView userTable = uiTableFactory.build(
                UiTableDefinition.builder()
                        .title("User List")
                        .description("Review user accounts, statuses, and audit timestamps.")
                        .emptyMessage("No users found.")
                        .pagination(pagination)
                        .build(),
                rows,
                UserTableRowView.class,
                row -> canManageRbac
                        ? List.of(UiTableActionView.builder()
                                .label("Roles")
                                .path(appProperties.getUi().getHomePath() + "/users?detailUserId=" + row.getId())
                                .buttonClass("btn-outline-primary")
                                .build())
                        : List.of());

        UiModalView createUserModal = canCreateUser
                ? uiModalFactory.build(
                        UiModalDefinition.builder()
                                .id("create-user-modal")
                                .title("Create User")
                                .description("Add a new user account.")
                                .triggerLabel("New User")
                                .triggerButtonClass("btn-primary")
                                .actionPath(appProperties.getUi().getHomePath() + "/users")
                                .submitLabel("Create User")
                                .build(),
                        CreateUserModalForm.class,
                        form,
                        Map.of(),
                        modalErrors == null ? Map.of() : modalErrors)
                : null;

        UiAssignmentDetailModalView detailModal = canManageRbac && detailUserId != null
                ? buildUserRoleDetailModal(detailUserId)
                : null;

        return UserListPageView.builder()
                .title("User Management")
                .heading("Users")
                .description("Create and review user accounts from the administration workspace.")
                .shell(buildShell(currentUser, request))
                .userTable(userTable)
                .createUserModal(createUserModal)
                .detailModal(detailModal)
                .errorMessage(errorMessage)
                .openCreateUserModal(openCreateUserModal)
                .openDetailModal(openDetailModal && detailModal != null)
                .build();
    }

    private UiAssignmentDetailModalView buildUserRoleDetailModal(UUID userId) {
        UserDetailResult user = userService.getUserDetailById(userId);

        List<RoleResult> assignedRoles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                        .sorted(Comparator.comparing(
                                (RoleResult role) -> role.getKey(),
                                String.CASE_INSENSITIVE_ORDER))
                        .toList();

        Set<String> assignedRoleIds = assignedRoles.stream()
                .map(role -> role.getId())
                .collect(Collectors.toSet());

        List<RoleResult> allRoles = rbacService.getManyRoles(
                new RoleFilterCriteria(),
                Pageable.unpaged())
                .getContent()
                .stream()
                .sorted(Comparator.comparing(
                        (RoleResult role) -> role.getKey(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<UiAssignmentDetailItemView> assignedItems = assignedRoles.stream()
                .map(role -> UiAssignmentDetailItemView.builder()
                        .title(role.getKey())
                        .description(role.getName())
                        .actionPath(appProperties.getUi().getHomePath() + "/users/" + userId + "/detail/remove-role")
                        .actionLabel("Remove")
                        .actionButtonClass("btn-outline-danger")
                        .hiddenFieldName("roleId")
                        .hiddenFieldValue(role.getId())
                        .build())
                .toList();

        List<UiAssignmentDetailItemView> availableItems = allRoles.stream()
                .filter(role -> !assignedRoleIds.contains(role.getId()))
                .map(role -> UiAssignmentDetailItemView.builder()
                        .title(role.getKey())
                        .description(role.getName())
                        .actionPath(appProperties.getUi().getHomePath() + "/users/" + userId + "/detail/assign-role")
                        .actionLabel("Assign")
                        .actionButtonClass("btn-outline-primary")
                        .hiddenFieldName("roleId")
                        .hiddenFieldValue(role.getId())
                        .build())
                .toList();

        return UiAssignmentDetailModalView.builder()
                .id("user-role-detail-modal")
                .title("User Roles")
                .metadata(List.of(
                        UiAssignmentDetailMetaView.builder()
                                .label("Email")
                                .value(user.getEmail())
                                .monospace(false)
                                .build(),
                        UiAssignmentDetailMetaView.builder()
                                .label("Status")
                                .value(String.valueOf(user.getStatus()))
                                .monospace(false)
                                .build()))
                .assignedTitle("Assigned Roles")
                .assignedEmptyMessage("No roles assigned.")
                .assignedItems(assignedItems)
                .availableTitle("Available Roles")
                .availableEmptyMessage("All roles are already assigned.")
                .availableItems(availableItems)
                .build();
    }

    private CreateUserPayload toCreateUserPayload(CreateUserModalForm form) {
        CreateUserPayload payload = new CreateUserPayload();
        payload.setEmail(form.getEmail());
        payload.setPassword(form.getPassword());
        return payload;
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

    private UserTableRowView toRowView(UserResult result) {
        return UserTableRowView.builder()
                .id(result.getId())
                .email(result.getEmail())
                .status(result.getStatus())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }

    private boolean hasAuthority(UserPrincipal currentUser, String authority) {
        return currentUser.getAuthorities().stream()
                .anyMatch(item -> authority.equals(item.getAuthority()));
    }
}
