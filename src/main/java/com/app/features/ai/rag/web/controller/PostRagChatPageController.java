package com.app.features.ai.rag.web.controller;

import java.util.Locale;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.app.config.ratelimit.RateLimitPolicy;
import com.app.config.ratelimit.RateLimited;
import com.app.config.settings.AppProperties;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.security.UserPrincipal;
import com.app.features.ai.rag.schema.model.PostRagResult;
import com.app.features.ai.rag.schema.payload.PostRagQuestionPayload;
import com.app.features.ai.rag.service.PostRagService;
import com.app.features.ai.rag.support.PostRagLanguageResolver;
import com.app.features.ai.rag.web.support.PostRagChatMessageViewFactory;
import com.app.features.ai.rag.web.view.PostRagChatMessageView;
import com.app.features.ai.rag.web.view.PostRagChatPageView;
import com.app.features.ui.web.support.SocialShellFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("${app.ui.ai-chat-path:/ai-chat}")
public class PostRagChatPageController {

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final SocialShellFactory socialShellFactory;
    private final PostRagService postRagSvc;
    private final PostRagLanguageResolver postRagLanguageResolver;
    private final PostRagChatMessageViewFactory chatMessageViewFactory;

    @GetMapping
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            Model model) {
        PostRagChatPageView page = PostRagChatPageView.builder()
                .title(messageResolver.get("ai.chat.page.title"))
                .askPath(appProperties.getUi().getAiChatPath()
                        + "/ask/stream")
                .sessionStorageKey("vibe.ai.chat.v1."
                        + (currentUser == null
                                ? "guest"
                                : currentUser.getUserId()))
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .build();

        model.addAttribute(PostRagChatPageView.ATTRIBUTE, page);
        return "ai/rag/chat";
    }

    @RateLimited(RateLimitPolicy.RAG_QUERY)
    @PostMapping("/ask")
    public String answer(
            @Valid @ModelAttribute PostRagQuestionPayload question,
            Locale locale,
            Model model) {
        PostRagResult result = postRagSvc.answer(
                question.getQuestion(),
                postRagLanguageResolver.resolve(locale));
        model.addAttribute(
                PostRagChatMessageView.ATTRIBUTE,
                chatMessageViewFactory.build(result));
        return "ai/rag/fragments/chat-message"
                + " :: assistantMessage (message=${message})";
    }
}
