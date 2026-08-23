package com.app.features.post.aimoderation.web.controller;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.config.security.web.HtmxRequestSupport;
import com.app.config.settings.AppProperties;
import com.app.core.constant.PermissionConstants;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.security.UserPrincipal;
import com.app.features.post.aimoderation.enums.PostAiModerationMode;
import com.app.features.post.aimoderation.service.PostAiModerationAdminService;
import com.app.features.post.aimoderation.support.PostAiModerationCapability;
import com.app.features.post.aimoderation.web.support.PostAiModerationPageViewFactory;
import com.app.features.post.aimoderation.web.view.PostAiModerationConfigForm;
import com.app.features.post.aimoderation.web.view.PostAiModerationConfigPageView;
import com.app.features.post.aimoderation.web.view.PostAiModerationPanelState;
import com.app.features.post.aimoderation.web.view.PostAiModerationPanelView;
import com.app.features.ui.web.support.UiFormSubmitResult;
import com.app.features.ui.web.support.UiFormSubmitSupport;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@Secured(PermissionConstants.POST_MODERATE)
@RequestMapping("${app.ui.home-path:/admin}/posts/ai-moderation")
public class PostAiModerationPageController {

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final PostAiModerationAdminService postAiModerationAdminSvc;
    private final PostAiModerationCapability postAiModerationCapability;
    private final PostAiModerationPageViewFactory
            postAiModerationPageViewFactory;
    private final UiFormSubmitSupport uiFormSubmitSupport;

    @GetMapping
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            Model model) {
        Object stateAttribute = model.getAttribute(
                PostAiModerationPanelState.ATTRIBUTE);
        PostAiModerationPanelState state =
                stateAttribute instanceof PostAiModerationPanelState panelState
                        ? panelState
                        : null;
        model.addAttribute(
                PostAiModerationConfigPageView.ATTRIBUTE,
                postAiModerationPageViewFactory.buildConfigPage(
                        currentUser,
                        request.getRequestURI(),
                        state));
        return "post/ai-moderation/index";
    }

    @PostMapping("/config")
    public String updateConfig(
            HttpServletRequest request,
            @Valid @ModelAttribute("form")
            PostAiModerationConfigForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (form.getMode() == PostAiModerationMode.AUTO) {
            if (!postAiModerationCapability.isEnabled()) {
                bindingResult.rejectValue(
                        "mode",
                        "error.post.aiModerationDisabled",
                        messageResolver.get(
                                "error.post.aiModerationDisabled"));
            } else if (!StringUtils.hasText(form.getPromptText())) {
                bindingResult.rejectValue(
                        "promptText",
                        "validation.post.aiModeration.prompt.required",
                        messageResolver.get(
                                "validation.post.aiModeration.prompt.required"));
            }
        }

        UiFormSubmitResult submitResult = uiFormSubmitSupport.submit(
                bindingResult,
                () -> postAiModerationAdminSvc.updateConfig(
                        form.getMode(),
                        form.getPromptText()));
        PostAiModerationPanelState state =
                PostAiModerationPanelState.builder()
                        .form(submitResult.success() ? null : form)
                        .fieldErrors(submitResult.fieldErrors())
                        .saved(submitResult.success())
                        .build();

        if (HtmxRequestSupport.isHtmxRequest(request)) {
            model.addAttribute(
                    PostAiModerationPanelView.ATTRIBUTE,
                    postAiModerationPageViewFactory.buildPanel(state));
            return "post/ai-moderation/fragments/config"
                    + " :: panel (aiModeration=${aiModeration})";
        }

        redirectAttributes.addFlashAttribute(
                PostAiModerationPanelState.ATTRIBUTE,
                state);
        return "redirect:" + getAiModerationPath();
    }

    private String getAiModerationPath() {
        return appProperties.getUi().getHomePath()
                + "/posts/ai-moderation";
    }
}
