package com.app.features.rbac.web.controller;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.app.core.constant.PermissionConstants;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.rbac.schema.filter.PermissionFilterCriteria;
import com.app.features.rbac.schema.result.PermissionResult;
import com.app.features.rbac.service.RbacService;
import com.app.features.rbac.web.view.PermissionFilter;
import com.app.features.rbac.web.view.PermissionListPageView;
import com.app.features.rbac.web.view.PermissionTableRowView;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.support.UiTableFactory;
import com.app.features.ui.web.component.view.UiHtmxNavigationView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.component.view.UiTableDefinition;
import com.app.features.ui.web.component.view.UiTableView;
import com.app.features.ui.web.support.UiShellFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/rbac/permissions")
public class PermissionPageController {

    private static final String PERMISSION_TABLE_ID = "permission-table";

    private static final UiPageDefaults PERMISSION_PAGE_DEFAULTS = UiPageDefaults.builder()
            .page(0)
            .size(10)
            .sortBy("key")
            .sortDirection(Sort.Direction.ASC)
            .build();

    private final AppMessageResolver messageResolver;
    private final UiShellFactory uiShellFactory;
    private final RbacService rbacSvc;
    private final UiPaginationFactory uiPaginationFactory;
    private final UiPaginationPathBuilder uiPaginationPathBuilder;
    private final UiTableFactory uiTableFactory;
    private final ModelMapper mapper;

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

        var permissionPage = rbacSvc.getManyPermissions(criteria, query.toPageable(PERMISSION_PAGE_DEFAULTS));
        List<PermissionTableRowView> rows = permissionPage.getContent().stream()
                .map((PermissionResult permission) -> mapper.map(permission,
                        PermissionTableRowView.class))
                .toList();

        UiPaginationView pagination = uiPaginationFactory.build(
                permissionPage,
                uiPaginationPathBuilder.build(request, query, PERMISSION_PAGE_DEFAULTS),
                UiHtmxNavigationView.forComponent(PERMISSION_TABLE_ID));

        UiTableView permissionTable = uiTableFactory.build(
                UiTableDefinition.builder()
                        .id(PERMISSION_TABLE_ID)
                        .title(messageResolver.get("rbac.permission.table.title"))
                        .description(messageResolver.get("rbac.permission.table.description"))
                        .emptyMessage(messageResolver.get("rbac.permission.table.empty"))
                        .pagination(pagination)
                        .build(),
                rows,
                PermissionTableRowView.class);

        return PermissionListPageView.builder()
                .title(messageResolver.get("rbac.permission.page.title"))
                .shell(uiShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .permissionTable(permissionTable)
                .build();
    }
}
