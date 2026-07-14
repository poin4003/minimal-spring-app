package com.app.features.rbac.web.controller;

import java.util.List;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.app.config.settings.AppProperties;
import com.app.core.constant.PermissionConstants;
import com.app.core.menu.MenuService;
import com.app.core.security.UserPrincipal;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.features.rbac.schema.filter.PermissionFilterCriteria;
import com.app.features.rbac.schema.result.PermissionResult;
import com.app.features.rbac.service.RbacService;
import com.app.features.rbac.web.view.PermissionFilter;
import com.app.features.rbac.web.view.PermissionListPageView;
import com.app.features.rbac.web.view.PermissionTableRowView;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.support.UiTableFactory;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.component.view.UiTableDefinition;
import com.app.features.ui.web.component.view.UiTableView;
import com.app.features.ui.web.view.UiCurrentUserView;
import com.app.features.ui.web.view.UiShellView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/rbac/permissions")
public class PermissionPageController {

    private static final UiPageDefaults PERMISSION_PAGE_DEFAULTS = UiPageDefaults.builder()
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
    private final UiTableFactory uiTableFactory;

    @GetMapping
    @Secured(PermissionConstants.RBAC_MANAGE)
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @Valid @ModelAttribute("filter") PermissionFilter filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        model.addAttribute(
                PermissionListPageView.ATTRIBUTE,
                buildPage(currentUser, request, filter, query));
        return "rbac/permission/index";
    }

    private PermissionListPageView buildPage(
            UserPrincipal currentUser,
            HttpServletRequest request,
            PermissionFilter filter,
            UiPageQuery query) {
        PermissionFilterCriteria criteria = new PermissionFilterCriteria();
        criteria.setRoleId(filter.getRoleId());

        var permissionPage = rbacService.getManyPermissions(criteria, query.toPageable(PERMISSION_PAGE_DEFAULTS));
        List<PermissionTableRowView> rows = permissionPage.getContent().stream()
                .map((PermissionResult permission) -> this.toRowView(permission))
                .toList();

        UiPaginationView pagination = uiPaginationFactory.build(
                permissionPage,
                uiPaginationPathBuilder.build(request, query, PERMISSION_PAGE_DEFAULTS));

        UiTableView permissionTable = uiTableFactory.build(
                UiTableDefinition.builder()
                        .title("Permission List")
                        .description("Review permission identifiers and display names.")
                        .emptyMessage("No permissions found.")
                        .pagination(pagination)
                        .build(),
                rows,
                PermissionTableRowView.class);

        return PermissionListPageView.builder()
                .title("Permission Management")
                .heading("Permissions")
                .description("Review permission keys used by authorization rules.")
                .shell(buildShell(currentUser, request))
                .permissionTable(permissionTable)
                .build();
    }

    private PermissionTableRowView toRowView(PermissionResult result) {
        return PermissionTableRowView.builder()
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
