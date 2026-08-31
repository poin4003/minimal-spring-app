package com.app.features.ai.search.web.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.config.settings.AppProperties;
import com.app.core.i18n.AppMessageResolver;
import com.app.core.security.UserPrincipal;
import com.app.features.ai.search.web.view.PostSearchPageView;
import com.app.features.ui.web.support.SocialShellFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Controller
@Validated
@RequiredArgsConstructor
@RequestMapping("${app.ui.search-path:/search}")
public class PostSearchPageController {

    private final AppProperties appProperties;
    private final AppMessageResolver messageResolver;
    private final SocialShellFactory socialShellFactory;

    @GetMapping
    public String index(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "q", defaultValue = "")
            @Size(
                    max = 2000,
                    message = "{validation.ai.search.query.max}") String query,
            HttpServletRequest request,
            Model model) {
        PostSearchPageView page = PostSearchPageView.builder()
                .title(messageResolver.get("ai.search.page.title"))
                .searchPath(appProperties.getUi().getSearchPath())
                .resultsPath(appProperties.getUi().getSearchPath()
                        + "/results")
                .query(query.trim())
                .shell(socialShellFactory.build(
                        currentUser,
                        request.getRequestURI()))
                .build();
        model.addAttribute(PostSearchPageView.ATTRIBUTE, page);
        return "ai/search/index";
    }
}
