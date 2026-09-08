package com.app.features.post.catalogpost.web.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import com.app.config.security.web.HtmxRequestSupport;
import com.app.config.settings.AppProperties;
import com.app.core.constant.PermissionConstants;
import com.app.core.enums.RecordStatus;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.post.catalogpost.entity.CatalogAttributeEntity_;
import com.app.features.post.catalogpost.entity.CatalogCategoryEntity_;
import com.app.features.post.catalogpost.schema.filter.CatalogAttributeFilterCriteria;
import com.app.features.post.catalogpost.schema.filter.CatalogCategoryFilterCriteria;
import com.app.features.post.catalogpost.schema.payload.CatalogCategoryAttributePayload;
import com.app.features.post.catalogpost.schema.payload.CreateCatalogCategoryPayload;
import com.app.features.post.catalogpost.schema.payload.ReplaceCatalogCategoryAttributesPayload;
import com.app.features.post.catalogpost.schema.payload.UpdateCatalogCategoryPayload;
import com.app.features.post.catalogpost.schema.result.CatalogAttributeDefinitionResult;
import com.app.features.post.catalogpost.schema.result.CatalogCategoryAttributeResult;
import com.app.features.post.catalogpost.schema.result.CatalogCategoryResult;
import com.app.features.post.catalogpost.service.CatalogAttributeService;
import com.app.features.post.catalogpost.service.CatalogCategoryService;
import com.app.features.post.catalogpost.web.view.CatalogCategoryAdminPageView;
import com.app.features.post.catalogpost.web.view.CatalogCategoryAttributeAssignmentForm;
import com.app.features.post.catalogpost.web.view.CatalogCategoryForm;
import com.app.features.post.catalogpost.web.view.CatalogCategorySchemaAdminPageView;
import com.app.features.post.catalogpost.web.view.CatalogCategorySchemaForm;
import com.app.features.ui.web.component.support.UiPaginationFactory;
import com.app.features.ui.web.component.support.UiPaginationPathBuilder;
import com.app.features.ui.web.component.view.UiHtmxNavigationView;
import com.app.features.ui.web.component.view.UiPaginationView;
import com.app.features.ui.web.support.UiFormSubmitResult;
import com.app.features.ui.web.support.UiFormSubmitSupport;
import com.app.features.ui.web.support.UiShellFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.home-path:/admin}/catalog/categories")
public class CatalogCategoryPageController {

    private static final String LIST_ID = "catalog-category-list";
    private static final String SCHEMA_LIST_ID =
            "catalog-category-schema-list";
    private static final int PARENT_OPTION_LIMIT = 25;
    private static final UiPageDefaults PAGE_DEFAULTS = UiPageDefaults.builder()
            .page(0)
            .size(20)
            .sortBy(CatalogCategoryEntity_.SORT_ORDER)
            .sortDirection(Sort.Direction.ASC)
            .build();
    private static final UiPageDefaults SCHEMA_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(20)
                    .sortBy(CatalogAttributeEntity_.NAME)
                    .sortDirection(Sort.Direction.ASC)
                    .build();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final UiShellFactory uiShellFactory;
    private final UiPaginationFactory paginationFactory;
    private final UiPaginationPathBuilder paginationPathBuilder;
    private final UiFormSubmitSupport formSubmitSupport;
    private final CatalogCategoryService categorySvc;
    private final CatalogAttributeService attributeSvc;
    private final ModelMapper mapper;

    @GetMapping
    @Secured(PermissionConstants.CATALOG_CONFIGURE)
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @ModelAttribute("filter") CatalogCategoryFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @RequestParam(required = false) UUID editId,
            @RequestParam(required = false) String parentText,
            Model model) {
        CatalogCategoryForm form = editId == null
                ? newCategoryForm()
                : toForm(categorySvc.getCategory(editId));
        model.addAttribute("form", form);
        addPage(model, currentUser, request, filter, query, editId,
                parentText, form.getParentId(), Map.of());
        return "post/catalog/admin/categories";
    }

    @PostMapping
    @Secured(PermissionConstants.CATALOG_CONFIGURE)
    public String create(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response,
            @ModelAttribute("filter") CatalogCategoryFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @RequestParam(required = false) String parentText,
            @Valid @ModelAttribute("form") CatalogCategoryForm form,
            BindingResult bindingResult,
            Model model) {
        UiFormSubmitResult result = formSubmitSupport.submit(
                bindingResult,
                () -> categorySvc.createCategory(toCreatePayload(form)));
        if (result.success()) {
            return redirect(request, response, categoriesPath());
        }

        addPage(model, currentUser, request, filter, query, null,
                parentText, form.getParentId(), result.fieldErrors());
        return "post/catalog/admin/categories";
    }

    @PostMapping("/{categoryId}")
    @Secured(PermissionConstants.CATALOG_CONFIGURE)
    public String update(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable UUID categoryId,
            @ModelAttribute("filter") CatalogCategoryFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @RequestParam(required = false) String parentText,
            @Valid @ModelAttribute("form") CatalogCategoryForm form,
            BindingResult bindingResult,
            Model model) {
        UiFormSubmitResult result = formSubmitSupport.submit(
                bindingResult,
                () -> categorySvc.updateCategory(
                        categoryId,
                        toUpdatePayload(form)));
        if (result.success()) {
            return redirect(request, response, categoriesPath());
        }

        addPage(model, currentUser, request, filter, query, categoryId,
                parentText, form.getParentId(), result.fieldErrors());
        return "post/catalog/admin/categories";
    }

    @PostMapping("/{categoryId}/status")
    @Secured(PermissionConstants.CATALOG_CONFIGURE)
    public String updateStatus(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable UUID categoryId,
            @RequestParam RecordStatus status) {
        categorySvc.updateCategoryStatus(categoryId, status);
        return redirect(request, response, categoriesPath());
    }

    @GetMapping("/{categoryId}/attributes")
    @Secured(PermissionConstants.CATALOG_CONFIGURE)
    public String attributes(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @PathVariable UUID categoryId,
            @ModelAttribute("attributeFilter")
            CatalogAttributeFilterCriteria attributeFilter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        Page<CatalogAttributeDefinitionResult> attributePage = attributeSvc
                .getAttributes(
                        attributeFilter,
                        query.toPageable(SCHEMA_PAGE_DEFAULTS));
        model.addAttribute(
                "form",
                buildSchemaForm(categoryId, attributePage.getContent()));
        addSchemaPage(
                model,
                currentUser,
                request,
                categoryId,
                query,
                attributePage);
        return "post/catalog/admin/category-attributes";
    }

    @PostMapping("/{categoryId}/attributes")
    @Secured(PermissionConstants.CATALOG_CONFIGURE)
    public String replaceAttributes(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable UUID categoryId,
            @ModelAttribute("attributeFilter")
            CatalogAttributeFilterCriteria attributeFilter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @Valid @ModelAttribute("form") CatalogCategorySchemaForm form,
            BindingResult bindingResult,
            Model model) {
        UiFormSubmitResult result = formSubmitSupport.submit(
                bindingResult,
                () -> categorySvc.updateCategoryAttributeSelection(
                        categoryId,
                        form.getAssignments().stream()
                                .map(CatalogCategoryAttributeAssignmentForm
                                        ::getAttributeId)
                                .toList(),
                        toReplacePayload(form)));
        if (result.success()) {
            int currentPage = query.applyDefaults(SCHEMA_PAGE_DEFAULTS)
                    .getPage();
            return redirect(
                    request,
                    response,
                    paginationPathBuilder.build(
                            request,
                            query,
                            SCHEMA_PAGE_DEFAULTS)
                            .apply(currentPage));
        }

        Page<CatalogAttributeDefinitionResult> attributePage = attributeSvc
                .getAttributes(
                        attributeFilter,
                        query.toPageable(SCHEMA_PAGE_DEFAULTS));
        addSchemaPage(
                model,
                currentUser,
                request,
                categoryId,
                query,
                attributePage);
        model.addAttribute("fieldErrors", result.fieldErrors());
        return "post/catalog/admin/category-attributes";
    }

    private void addPage(
            Model model,
            UserPrincipal currentUser,
            HttpServletRequest request,
            CatalogCategoryFilterCriteria filter,
            UiPageQuery query,
            UUID editId,
            String parentText,
            UUID selectedParentId,
            Map<String, String> fieldErrors) {
        var categoryPage = categorySvc.getCategories(
                filter,
                query.toPageable(PAGE_DEFAULTS));
        UiPaginationView pagination = paginationFactory.build(
                categoryPage,
                paginationPathBuilder.build(request, query, PAGE_DEFAULTS),
                UiHtmxNavigationView.forComponent(LIST_ID));
        List<CatalogCategoryResult> parentOptions = loadParentOptions(
                editId,
                selectedParentId,
                parentText);

        model.addAttribute(
                CatalogCategoryAdminPageView.ATTRIBUTE,
                CatalogCategoryAdminPageView.builder()
                        .title(messageResolver.get(
                                "catalog.category.page.title"))
                        .shell(uiShellFactory.build(
                                currentUser,
                                request.getRequestURI()))
                        .basePath(categoriesPath())
                        .attributePath(appProperties.getUi().getHomePath()
                                + "/catalog/attributes")
                        .editId(editId)
                        .categories(categoryPage.getContent())
                        .parentOptions(parentOptions)
                        .parentText(parentText)
                        .pagination(pagination)
                        .fieldErrors(fieldErrors)
                        .build());
    }

    private void addSchemaPage(
            Model model,
            UserPrincipal currentUser,
            HttpServletRequest request,
            UUID categoryId,
            UiPageQuery query,
            Page<CatalogAttributeDefinitionResult> attributePage) {
        UiPaginationView pagination = paginationFactory.build(
                attributePage,
                paginationPathBuilder.build(
                        request,
                        query,
                        SCHEMA_PAGE_DEFAULTS),
                UiHtmxNavigationView.forComponent(SCHEMA_LIST_ID));
        model.addAttribute(
                CatalogCategorySchemaAdminPageView.ATTRIBUTE,
                CatalogCategorySchemaAdminPageView.builder()
                        .title(messageResolver.get(
                                "catalog.categorySchema.page.title"))
                        .shell(uiShellFactory.build(
                                currentUser,
                                request.getRequestURI()))
                        .basePath(categoriesPath())
                        .category(categorySvc.getCategory(categoryId))
                        .pagination(pagination)
                        .build());
    }

    private CatalogCategorySchemaForm buildSchemaForm(
            UUID categoryId,
            List<CatalogAttributeDefinitionResult> attributes) {
        Map<UUID, CatalogCategoryAttributeResult> configured = categorySvc
                .getCategoryConfiguration(categoryId)
                .getAttributes()
                .stream()
                .collect(Collectors.toMap(
                        item -> item.getAttribute().getId(),
                        Function.identity()));
        int nextPosition = configured.values().stream()
                .mapToInt(CatalogCategoryAttributeResult::getSortOrder)
                .max()
                .orElse(-1) + 1;

        CatalogCategorySchemaForm form = new CatalogCategorySchemaForm();
        List<CatalogCategoryAttributeAssignmentForm> assignments =
                new ArrayList<>();
        for (CatalogAttributeDefinitionResult attribute : attributes) {
            CatalogCategoryAttributeResult mapping = configured.get(
                    attribute.getId());
            assignments.add(toAssignmentForm(
                    attribute,
                    mapping,
                    mapping == null ? nextPosition++ : mapping.getSortOrder()));
        }
        assignments.sort(Comparator
                .comparing(CatalogCategoryAttributeAssignmentForm::isSelected)
                .reversed()
                .thenComparingInt(
                        CatalogCategoryAttributeAssignmentForm::getSortOrder)
                .thenComparing(CatalogCategoryAttributeAssignmentForm::getName));
        form.setAssignments(assignments);
        return form;
    }

    private List<CatalogCategoryResult> loadParentOptions(
            UUID editId,
            UUID selectedParentId,
            String parentText) {
        CatalogCategoryFilterCriteria parentFilter =
                new CatalogCategoryFilterCriteria();
        parentFilter.setText(parentText);
        parentFilter.setStatus(RecordStatus.ACTIVE);
        List<CatalogCategoryResult> options = new ArrayList<>(categorySvc
                .getCategories(
                        parentFilter,
                        PageRequest.of(
                                0,
                                PARENT_OPTION_LIMIT,
                                Sort.by(
                                        CatalogCategoryEntity_.SORT_ORDER,
                                        CatalogCategoryEntity_.NAME)))
                .getContent());
        options.removeIf(category -> category.getId().equals(editId));
        if (selectedParentId != null && options.stream()
                .noneMatch(category -> category.getId()
                        .equals(selectedParentId))) {
            options.add(categorySvc.getCategory(selectedParentId));
        }
        options.sort(Comparator
                .comparingInt(CatalogCategoryResult::getSortOrder)
                .thenComparing(CatalogCategoryResult::getName));
        return options;
    }

    private CatalogCategoryAttributeAssignmentForm toAssignmentForm(
            CatalogAttributeDefinitionResult attribute,
            CatalogCategoryAttributeResult mapping,
            int sortOrder) {
        CatalogCategoryAttributeAssignmentForm form =
                new CatalogCategoryAttributeAssignmentForm();
        form.setAttributeId(attribute.getId());
        form.setKey(attribute.getKey());
        form.setName(attribute.getName());
        form.setValueType(attribute.getValueType());
        form.setStatus(attribute.getStatus());
        form.setSelected(mapping != null);
        form.setRequired(mapping != null && mapping.isRequired());
        form.setFilterable(mapping != null && mapping.isFilterable());
        form.setMultipleValuesAllowed(
                mapping != null && mapping.isMultipleValuesAllowed());
        form.setCustomOptionAllowed(
                mapping != null && mapping.isCustomOptionAllowed());
        form.setSortOrder(sortOrder);
        return form;
    }

    private ReplaceCatalogCategoryAttributesPayload toReplacePayload(
            CatalogCategorySchemaForm form) {
        ReplaceCatalogCategoryAttributesPayload payload =
                new ReplaceCatalogCategoryAttributesPayload();
        payload.setAttributes(form.getAssignments().stream()
                .filter(CatalogCategoryAttributeAssignmentForm::isSelected)
                .map(this::toAssignmentPayload)
                .toList());
        return payload;
    }

    private CatalogCategoryAttributePayload toAssignmentPayload(
            CatalogCategoryAttributeAssignmentForm form) {
        CatalogCategoryAttributePayload payload =
                new CatalogCategoryAttributePayload();
        payload.setAttributeId(form.getAttributeId());
        payload.setRequired(form.isRequired());
        payload.setFilterable(form.isFilterable());
        payload.setMultipleValuesAllowed(form.isMultipleValuesAllowed());
        payload.setCustomOptionAllowed(form.isCustomOptionAllowed());
        payload.setSortOrder(form.getSortOrder());
        return payload;
    }

    private CatalogCategoryForm newCategoryForm() {
        CatalogCategoryForm form = new CatalogCategoryForm();
        form.setSortOrder(0);
        return form;
    }

    private CatalogCategoryForm toForm(CatalogCategoryResult category) {
        return mapper.map(category, CatalogCategoryForm.class);
    }

    private CreateCatalogCategoryPayload toCreatePayload(
            CatalogCategoryForm form) {
        return mapper.map(form, CreateCatalogCategoryPayload.class);
    }

    private UpdateCatalogCategoryPayload toUpdatePayload(
            CatalogCategoryForm form) {
        return mapper.map(form, UpdateCatalogCategoryPayload.class);
    }

    private String categoriesPath() {
        return appProperties.getUi().getHomePath() + "/catalog/categories";
    }

    private String redirect(
            HttpServletRequest request,
            HttpServletResponse response,
            String path) {
        return HtmxRequestSupport.redirectView(request, response, path);
    }
}
