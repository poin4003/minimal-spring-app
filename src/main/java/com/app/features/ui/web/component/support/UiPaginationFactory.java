package com.app.features.ui.web.component.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.app.features.ui.web.component.view.UiPaginationView;

@Component
public class UiPaginationFactory {

    private static final int WINDOW_RADIUS = 2;

    public UiPaginationView build(Page<?> page, IntFunction<String> pathBuilder) {
        int currentPage = page.getNumber();
        int totalPages = page.getTotalPages();

        if (totalPages <= 0) {
            return UiPaginationView.builder()
                    .currentPage(currentPage)
                    .totalPages(0)
                    .totalElements(page.getTotalElements())
                    .pageSize(page.getSize())
                    .hasPrevious(false)
                    .hasNext(false)
                    .items(List.of())
                    .build();
        }

        List<UiPaginationView.Item> items = new ArrayList<>();
        int startPage = Math.max(0, currentPage - WINDOW_RADIUS);
        int endPage = Math.min(totalPages - 1, currentPage + WINDOW_RADIUS);

        if (startPage > 0) {
            items.add(buildPageItem(0, currentPage, pathBuilder));
            if (startPage > 1) {
                items.add(buildEllipsisItem());
            }
        }

        for (int pageIndex = startPage; pageIndex <= endPage; pageIndex++) {
            items.add(buildPageItem(pageIndex, currentPage, pathBuilder));
        }

        if (endPage < totalPages - 1) {
            if (endPage < totalPages - 2) {
                items.add(buildEllipsisItem());
            }
            items.add(buildPageItem(totalPages - 1, currentPage, pathBuilder));
        }

        return UiPaginationView.builder()
                .currentPage(currentPage)
                .totalPages(totalPages)
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .hasPrevious(page.hasPrevious())
                .hasNext(page.hasNext())
                .previousPath(page.hasPrevious() ? pathBuilder.apply(currentPage - 1) : null)
                .nextPath(page.hasNext() ? pathBuilder.apply(currentPage + 1) : null)
                .items(items)
                .build();
    }

    private UiPaginationView.Item buildPageItem(
            int pageIndex,
            int currentPage,
            IntFunction<String> pathBuilder) {
        return UiPaginationView.Item.builder()
                .label(String.valueOf(pageIndex + 1))
                .path(pathBuilder.apply(pageIndex))
                .active(pageIndex == currentPage)
                .disabled(false)
                .build();
    }

    private UiPaginationView.Item buildEllipsisItem() {
        return UiPaginationView.Item.builder()
                .label("...")
                .disabled(true)
                .build();
    }
}
