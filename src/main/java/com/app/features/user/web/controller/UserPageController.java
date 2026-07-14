package com.app.features.user.web.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.app.features.rbac.schema.result.RoleResult;
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
            @RequestParam(required = false) UUID metadataUserId,
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
                        metadataUserId,
                        metadataUserId != null,
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
                        false,
                        null,
                        false));
        return "user/index";
    }

    private UserListPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            UserFilter filter,
            CreateUserModalForm form,
            Map<String, String> modalErrors,
            String errorMessage,
            boolean openCreateUserModal,
            UUID metadataUserId,
            boolean openMetadataModal,
            UUID detailUserId,
            boolean openDetailModal) {
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
                row -> List.of(
                        UiTableActionView.builder()
                                .label("Metadata")
                                .path(appProperties.getUi().getHomePath() + "/users?metadataUserId=" + row.getId())
                                .buttonClass("btn-outline-secondary")
                                .build(),
                        UiTableActionView.builder()
                                .label("Detail")
                                .path(appProperties.getUi().getHomePath() + "/users?detailUserId=" + row.getId())
                                .buttonClass("btn-outline-primary")
                                .build(),
                        UiTableActionView.builder()
                                .label("Manage Roles")
                                .path(appProperties.getUi().getHomePath() + "/users/" + row.getId() + "/roles?mode=ASSIGNED")
                                .buttonClass("btn-primary")
                                .build()));

        UiModalView createUserModal = uiModalFactory.build(
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
                modalErrors == null ? Map.of() : modalErrors);

        UiMetadataModalView metadataModal = metadataUserId == null
                ? null
                : buildUserMetadataModal(metadataUserId);

        UiDetailModalView detailModal = detailUserId == null
                ? null
                : buildUserDetailModal(detailUserId);

        return UserListPageView.builder()
                .title("User Management")
                .heading("Users")
                .description("Create and review user accounts from the administration workspace.")
                .shell(buildShell(currentUser, request))
                .userTable(userTable)
                .createUserModal(createUserModal)
                .metadataModal(metadataModal)
                .detailModal(detailModal)
                .errorMessage(errorMessage)
                .openCreateUserModal(openCreateUserModal)
                .openMetadataModal(openMetadataModal && metadataModal != null)
                .openDetailModal(openDetailModal && detailModal != null)
                .build();
    }

    private UiMetadataModalView buildUserMetadataModal(UUID userId) {
        UserDetailResult user = userService.getUserDetailById(userId);

        return UiMetadataModalView.builder()
                .id("user-metadata-modal")
                .title("User Metadata")
                .items(List.of(
                        UiMetadataItemView.builder()
                                .label("Email")
                                .value(user.getEmail())
                                .monospace(false)
                                .build(),
                        UiMetadataItemView.builder()
                                .label("Status")
                                .value(String.valueOf(user.getStatus()))
                                .monospace(false)
                                .build(),
                        UiMetadataItemView.builder()
                                .label("Created At")
                                .value(user.getCreatedAt())
                                .monospace(true)
                                .build(),
                        UiMetadataItemView.builder()
                                .label("Updated At")
                                .value(user.getUpdatedAt())
                                .monospace(true)
                                .build()))
                .build();
    }

    private UiDetailModalView buildUserDetailModal(UUID userId) {
        UserDetailResult user = userService.getUserDetailById(userId);

        List<UiDetailItemView> items = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                        .sorted(Comparator.comparing(
                                (RoleResult role) -> role.getKey(),
                                String.CASE_INSENSITIVE_ORDER))
                        .map(role -> UiDetailItemView.builder()
                                .title(role.getKey())
                                .description(role.getName())
                                .build())
                        .toList();

        return UiDetailModalView.builder()
                .id("user-detail-modal")
                .title("User Detail")
                .listTitle("Assigned Roles")
                .items(items)
                .emptyMessage("No roles assigned.")
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
}
