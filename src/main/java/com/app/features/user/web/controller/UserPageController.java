package com.app.features.user.web.controller;

import java.util.List;
import java.util.Map;

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
import com.app.features.ui.web.component.support.UiModalFactory;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.support.UiTableFactory;
import com.app.features.ui.web.component.view.UiModalDefinition;
import com.app.features.ui.web.component.view.UiModalView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.component.view.UiTableDefinition;
import com.app.features.ui.web.component.view.UiTableView;
import com.app.features.ui.web.support.UiFormSubmitResult;
import com.app.features.ui.web.support.UiFormSubmitSupport;
import com.app.features.ui.web.view.UiCurrentUserView;
import com.app.features.ui.web.view.UiShellView;
import com.app.features.user.schema.filter.UserFilter;
import com.app.features.user.schema.payload.CreateUserPayload;
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
            @RequestParam(defaultValue = "false") boolean created,
            Model model) {
        model.addAttribute(
                UserListPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        filter,
                        new CreateUserModalForm(),
                        null,
                        created ? "User created successfully." : null,
                        null,
                        false));
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
            return "redirect:" + appProperties.getUi().getHomePath() + "/users?created=true";
        }

        model.addAttribute(
                UserListPageView.ATTRIBUTE,
                buildPage(
                        currentUser,
                        request,
                        filter,
                        form,
                        submitResult.fieldErrors(),
                        null,
                        "Please correct the form and try again.",
                        true));
        return "user/index";
    }

    private UserListPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            UserFilter filter,
            CreateUserModalForm form,
            Map<String, String> modalErrors,
            String successMessage,
            String errorMessage,
            boolean openCreateUserModal) {
        var userPage = userService.getManyUser(filter.toPageable());
        List<UserTableRowView> rows = userPage.getContent().stream()
                .map(this::toRowView)
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
                UserTableRowView.class);

        UiModalView createUserModal = hasAuthority(currentUser, PermissionConstants.USER_CREATE)
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

        return UserListPageView.builder()
                .title("User Management")
                .heading("Users")
                .description("Create and review user accounts from the administration workspace.")
                .shell(buildShell(currentUser, request))
                .userTable(userTable)
                .createUserModal(createUserModal)
                .successMessage(successMessage)
                .errorMessage(errorMessage)
                .openCreateUserModal(openCreateUserModal)
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
