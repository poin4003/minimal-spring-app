package com.app.features.post.catalogpost.web.controller;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.modelmapper.ModelMapper;
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
import com.app.core.exception.ExceptionFactory;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.post.catalogpost.entity.CatalogAttributeEntity_;
import com.app.features.post.catalogpost.entity.CatalogAttributeOptionEntity_;
import com.app.features.post.catalogpost.enums.CatalogAttributeValueType;
import com.app.features.post.catalogpost.schema.filter.CatalogAttributeFilterCriteria;
import com.app.features.post.catalogpost.schema.payload.CreateCatalogAttributeOptionPayload;
import com.app.features.post.catalogpost.schema.payload.CreateCatalogAttributePayload;
import com.app.features.post.catalogpost.schema.payload.UpdateCatalogAttributeOptionPayload;
import com.app.features.post.catalogpost.schema.payload.UpdateCatalogAttributePayload;
import com.app.features.post.catalogpost.schema.result.CatalogAttributeDefinitionResult;
import com.app.features.post.catalogpost.schema.result.CatalogAttributeOptionResult;
import com.app.features.post.catalogpost.service.CatalogAttributeService;
import com.app.features.post.catalogpost.web.view.CatalogAttributeAdminPageView;
import com.app.features.post.catalogpost.web.view.CatalogAttributeForm;
import com.app.features.post.catalogpost.web.view.CatalogAttributeOptionForm;
import com.app.features.post.catalogpost.web.view.CatalogOptionAdminPageView;
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
@RequestMapping("${app.ui.home-path:/admin}/catalog/attributes")
public class CatalogAttributePageController {

    private static final String LIST_ID = "catalog-attribute-list";
    private static final String OPTION_LIST_ID = "catalog-option-list";
    private static final UiPageDefaults PAGE_DEFAULTS = UiPageDefaults.builder()
            .page(0)
            .size(20)
            .sortBy(CatalogAttributeEntity_.NAME)
            .sortDirection(Sort.Direction.ASC)
            .build();
    private static final UiPageDefaults OPTION_PAGE_DEFAULTS =
            UiPageDefaults.builder()
                    .page(0)
                    .size(20)
                    .sortBy(CatalogAttributeOptionEntity_.SORT_ORDER)
                    .sortDirection(Sort.Direction.ASC)
                    .build();

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final UiShellFactory uiShellFactory;
    private final UiPaginationFactory paginationFactory;
    private final UiPaginationPathBuilder paginationPathBuilder;
    private final UiFormSubmitSupport formSubmitSupport;
    private final CatalogAttributeService attributeSvc;
    private final ModelMapper mapper;

    @GetMapping
    @Secured(PermissionConstants.CATALOG_CONFIGURE)
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @ModelAttribute("filter") CatalogAttributeFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @RequestParam(required = false) UUID editId,
            Model model) {
        CatalogAttributeForm form = editId == null
                ? newAttributeForm()
                : toAttributeForm(attributeSvc.getAttribute(editId));
        model.addAttribute("form", form);
        addPage(model, currentUser, request, filter, query, editId, Map.of());
        return "post/catalog/admin/attributes";
    }

    @PostMapping
    @Secured(PermissionConstants.CATALOG_CONFIGURE)
    public String create(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response,
            @ModelAttribute("filter") CatalogAttributeFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @Valid @ModelAttribute("form") CatalogAttributeForm form,
            BindingResult bindingResult,
            Model model) {
        UiFormSubmitResult result = formSubmitSupport.submit(
                bindingResult,
                () -> attributeSvc.createAttribute(toCreatePayload(form)));
        if (result.success()) {
            return redirect(request, response, attributesPath());
        }

        addPage(model, currentUser, request, filter, query, null,
                result.fieldErrors());
        return "post/catalog/admin/attributes";
    }

    @PostMapping("/{attributeId}")
    @Secured(PermissionConstants.CATALOG_CONFIGURE)
    public String update(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable UUID attributeId,
            @ModelAttribute("filter") CatalogAttributeFilterCriteria filter,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @Valid @ModelAttribute("form") CatalogAttributeForm form,
            BindingResult bindingResult,
            Model model) {
        UiFormSubmitResult result = formSubmitSupport.submit(
                bindingResult,
                () -> attributeSvc.updateAttribute(
                        attributeId,
                        toUpdatePayload(form)));
        if (result.success()) {
            return redirect(request, response, attributesPath());
        }

        addPage(model, currentUser, request, filter, query, attributeId,
                result.fieldErrors());
        return "post/catalog/admin/attributes";
    }

    @PostMapping("/{attributeId}/status")
    @Secured(PermissionConstants.CATALOG_CONFIGURE)
    public String updateStatus(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable UUID attributeId,
            @RequestParam RecordStatus status) {
        attributeSvc.updateAttributeStatus(attributeId, status);
        return redirect(request, response, attributesPath());
    }

    @GetMapping("/{attributeId}/options")
    @Secured(PermissionConstants.CATALOG_CONFIGURE)
    public String options(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            @PathVariable UUID attributeId,
            @RequestParam(required = false) UUID editId,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        CatalogAttributeDefinitionResult attribute = requireOptionAttribute(
                attributeId);
        CatalogAttributeOptionForm form = editId == null
                ? newOptionForm()
                : toOptionForm(attribute, editId);
        model.addAttribute("form", form);
        addOptionPage(
                model,
                currentUser,
                request,
                attribute,
                editId,
                query,
                Map.of());
        return "post/catalog/admin/options";
    }

    @PostMapping("/{attributeId}/options")
    @Secured(PermissionConstants.CATALOG_CONFIGURE)
    public String createOption(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable UUID attributeId,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @Valid @ModelAttribute("form") CatalogAttributeOptionForm form,
            BindingResult bindingResult,
            Model model) {
        CatalogAttributeDefinitionResult attribute = requireOptionAttribute(
                attributeId);
        UiFormSubmitResult result = formSubmitSupport.submit(
                bindingResult,
                () -> attributeSvc.createOption(
                        attributeId,
                        toCreateOptionPayload(form)));
        if (result.success()) {
            return redirect(request, response, optionsPath(attributeId));
        }

        addOptionPage(model, currentUser, request, attribute, null, query,
                result.fieldErrors());
        return "post/catalog/admin/options";
    }

    @PostMapping("/{attributeId}/options/{optionId}")
    @Secured(PermissionConstants.CATALOG_CONFIGURE)
    public String updateOption(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable UUID attributeId,
            @PathVariable UUID optionId,
            @Valid @ModelAttribute("query") UiPageQuery query,
            @Valid @ModelAttribute("form") CatalogAttributeOptionForm form,
            BindingResult bindingResult,
            Model model) {
        CatalogAttributeDefinitionResult attribute = requireOptionAttribute(
                attributeId);
        UiFormSubmitResult result = formSubmitSupport.submit(
                bindingResult,
                () -> attributeSvc.updateOption(
                        attributeId,
                        optionId,
                        toUpdateOptionPayload(form)));
        if (result.success()) {
            return redirect(request, response, optionsPath(attributeId));
        }

        addOptionPage(model, currentUser, request, attribute, optionId, query,
                result.fieldErrors());
        return "post/catalog/admin/options";
    }

    @PostMapping("/{attributeId}/options/{optionId}/status")
    @Secured(PermissionConstants.CATALOG_CONFIGURE)
    public String updateOptionStatus(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable UUID attributeId,
            @PathVariable UUID optionId,
            @RequestParam RecordStatus status) {
        attributeSvc.updateOptionStatus(attributeId, optionId, status);
        return redirect(request, response, optionsPath(attributeId));
    }

    private void addPage(
            Model model,
            UserPrincipal currentUser,
            HttpServletRequest request,
            CatalogAttributeFilterCriteria filter,
            UiPageQuery query,
            UUID editId,
            Map<String, String> fieldErrors) {
        var attributePage = attributeSvc.getAttributes(
                filter,
                query.toPageable(PAGE_DEFAULTS));
        UiPaginationView pagination = paginationFactory.build(
                attributePage,
                paginationPathBuilder.build(request, query, PAGE_DEFAULTS),
                UiHtmxNavigationView.forComponent(LIST_ID));

        model.addAttribute(
                CatalogAttributeAdminPageView.ATTRIBUTE,
                CatalogAttributeAdminPageView.builder()
                        .title(messageResolver.get(
                                "catalog.attribute.page.title"))
                        .shell(uiShellFactory.build(
                                currentUser,
                                request.getRequestURI()))
                        .basePath(attributesPath())
                        .categoryPath(appProperties.getUi().getHomePath()
                                + "/catalog/categories")
                        .editId(editId)
                        .attributes(attributePage.getContent())
                        .valueTypes(Arrays.asList(
                                CatalogAttributeValueType.values()))
                        .pagination(pagination)
                        .fieldErrors(fieldErrors)
                        .build());
    }

    private void addOptionPage(
            Model model,
            UserPrincipal currentUser,
            HttpServletRequest request,
            CatalogAttributeDefinitionResult attribute,
            UUID editId,
            UiPageQuery query,
            Map<String, String> fieldErrors) {
        var optionPage = attributeSvc.getOptions(
                attribute.getId(),
                query.toPageable(OPTION_PAGE_DEFAULTS));
        UiPaginationView pagination = paginationFactory.build(
                optionPage,
                paginationPathBuilder.build(
                        request,
                        query,
                        OPTION_PAGE_DEFAULTS),
                UiHtmxNavigationView.forComponent(OPTION_LIST_ID));
        model.addAttribute(
                CatalogOptionAdminPageView.ATTRIBUTE,
                CatalogOptionAdminPageView.builder()
                        .title(messageResolver.get(
                                "catalog.option.page.title"))
                        .shell(uiShellFactory.build(
                                currentUser,
                                request.getRequestURI()))
                        .basePath(attributesPath())
                        .editId(editId)
                        .attribute(attribute)
                        .options(optionPage.getContent())
                        .pagination(pagination)
                        .fieldErrors(fieldErrors)
                        .build());
    }

    private CatalogAttributeDefinitionResult requireOptionAttribute(
            UUID attributeId) {
        CatalogAttributeDefinitionResult attribute = attributeSvc.getAttribute(
                attributeId);
        if (attribute.getValueType() != CatalogAttributeValueType.OPTION) {
            throw ExceptionFactory.invalidParam(
                    "error.catalogAttribute.optionTypeRequired",
                    attributeId);
        }
        return attribute;
    }

    private CatalogAttributeForm newAttributeForm() {
        CatalogAttributeForm form = new CatalogAttributeForm();
        form.setSearchable(true);
        form.setValueType(CatalogAttributeValueType.TEXT);
        return form;
    }

    private CatalogAttributeForm toAttributeForm(
            CatalogAttributeDefinitionResult attribute) {
        return mapper.map(attribute, CatalogAttributeForm.class);
    }

    private CatalogAttributeOptionForm newOptionForm() {
        CatalogAttributeOptionForm form = new CatalogAttributeOptionForm();
        form.setSortOrder(0);
        return form;
    }

    private CatalogAttributeOptionForm toOptionForm(
            CatalogAttributeDefinitionResult attribute,
            UUID optionId) {
        CatalogAttributeOptionResult option = attributeSvc.getOption(
                attribute.getId(),
                optionId);
        return mapper.map(option, CatalogAttributeOptionForm.class);
    }

    private CreateCatalogAttributePayload toCreatePayload(
            CatalogAttributeForm form) {
        return mapper.map(form, CreateCatalogAttributePayload.class);
    }

    private UpdateCatalogAttributePayload toUpdatePayload(
            CatalogAttributeForm form) {
        return mapper.map(form, UpdateCatalogAttributePayload.class);
    }

    private CreateCatalogAttributeOptionPayload toCreateOptionPayload(
            CatalogAttributeOptionForm form) {
        return mapper.map(form, CreateCatalogAttributeOptionPayload.class);
    }

    private UpdateCatalogAttributeOptionPayload toUpdateOptionPayload(
            CatalogAttributeOptionForm form) {
        return mapper.map(form, UpdateCatalogAttributeOptionPayload.class);
    }

    private String attributesPath() {
        return appProperties.getUi().getHomePath() + "/catalog/attributes";
    }

    private String optionsPath(UUID attributeId) {
        return attributesPath() + "/" + attributeId + "/options";
    }

    private String redirect(
            HttpServletRequest request,
            HttpServletResponse response,
            String path) {
        return HtmxRequestSupport.redirectView(request, response, path);
    }
}
