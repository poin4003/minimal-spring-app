package com.app.features.post.aimoderation.web.controller;

import java.util.UUID;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.app.core.constant.PermissionConstants;
import com.app.core.schema.query.UiPageQuery;
import com.app.core.security.UserPrincipal;
import com.app.features.post.aimoderation.web.support.PostAiModerationPageViewFactory;
import com.app.features.post.aimoderation.web.view.PostAiModerationDecisionLogModalView;
import com.app.features.post.aimoderation.web.view.PostAiModerationLogPageView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@Secured(PermissionConstants.POST_MODERATE)
@RequestMapping(
        "${app.ui.home-path:/admin}/posts/moderation/{postId}/ai-logs")
public class PostAiModerationLogPageController {

    private final PostAiModerationPageViewFactory
            postAiModerationPageViewFactory;

    @GetMapping
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID postId,
            HttpServletRequest request,
            @Valid @ModelAttribute("query") UiPageQuery query,
            Model model) {
        model.addAttribute(
                PostAiModerationLogPageView.ATTRIBUTE,
                postAiModerationPageViewFactory.buildLogPage(
                        currentUser,
                        request.getRequestURI(),
                        postId,
                        query));
        return "post/ai-moderation/logs";
    }

    @GetMapping("/{logId}")
    public String detail(
            @PathVariable UUID postId,
            @PathVariable UUID logId,
            Model model) {
        model.addAttribute(
                PostAiModerationDecisionLogModalView.ATTRIBUTE,
                postAiModerationPageViewFactory
                        .buildDecisionLogModal(postId, logId));
        return "post/ai-moderation/fragments/decision-log-modal"
                + " :: modal (modal=${decisionLogModal})";
    }
}
