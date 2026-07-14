package com.app.features.ui.web.component.support;

import java.util.Set;
import java.util.function.IntFunction;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class UiPaginationPathBuilder {

    private static final Set<String> PAGING_KEYS = Set.of(
            "page",
            "size",
            "sortBy",
            "sortDirection");

    public IntFunction<String> build(
            HttpServletRequest request,
            UiPageQuery query,
            UiPageDefaults defaults) {
        String requestUri = request.getRequestURI();
        int pageSize = query.getSize() == null ? defaults.getSize() : query.getSize();

        return pageNumber -> {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath(requestUri);

            request.getParameterMap().forEach((key, values) -> {
                if (PAGING_KEYS.contains(key)) {
                    return;
                }

                for (String value : values) {
                    builder.queryParam(key, value);
                }
            });

            builder.queryParam("page", pageNumber);
            builder.queryParam("size", pageSize);
            builder.queryParam(
                    "sortBy",
                    query.getSortBy() == null || query.getSortBy().isBlank()
                            ? defaults.getSortBy()
                            : query.getSortBy());
            builder.queryParam(
                    "sortDirection",
                    (query.getSortDirection() == null
                            ? defaults.getSortDirection()
                            : query.getSortDirection()).name());

            return builder.build().encode().toUriString();
        };
    }
}
