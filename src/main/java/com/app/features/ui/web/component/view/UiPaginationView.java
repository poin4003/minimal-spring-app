package com.app.features.ui.web.component.view;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiPaginationView {

    private final int currentPage;
    private final int totalPages;
    private final long totalElements;
    private final int pageSize;
    private final boolean hasPrevious;
    private final boolean hasNext;
    private final String previousPath;
    private final String nextPath;
    private final List<Item> items;

    @Getter
    @Builder
    public static class Item {
        private final String label;
        private final String path;
        private final boolean active;
        private final boolean disabled;
    }
}
