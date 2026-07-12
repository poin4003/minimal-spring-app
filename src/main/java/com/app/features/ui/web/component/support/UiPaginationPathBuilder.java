package com.app.features.ui.web.component.support;

import java.util.function.IntFunction;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.core.schema.filter.BasePageFilter;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class UiPaginationPathBuilder {

    public IntFunction<String> build(HttpServletRequest request, BasePageFilter filter) {
        String requestUri = request.getRequestURI();
        int pageSize = filter == null || filter.getSize() == null
                ? 10
                : filter.getSize();

        return pageNumber -> {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath(requestUri);

            request.getParameterMap().forEach((key, values) -> {
                if ("page".equals(key) || "size".equals(key)) {
                    return;
                }

                for (String value : values) {
                    builder.queryParam(key, value);
                }
            });

            builder.queryParam("page", pageNumber);
            builder.queryParam("size", pageSize);

            return builder.build().encode().toUriString();
        };
    }
}
