package com.app.core.schema.query;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UiPageQuery {

    @Min(value = 0, message = "Page must be greater than or equal to 0")
    private Integer page;

    @Min(value = 1, message = "Size must be greater than or equal to 1")
    @Max(value = 100, message = "Size must be less than or equal to 100")
    private Integer size;

    private String sortBy;

    private Sort.Direction sortDirection;

    public Pageable toPageable(UiPageDefaults defaults) {
        return PageRequest.of(
                resolvePage(defaults),
                resolveSize(defaults),
                Sort.by(resolveSortDirection(defaults), resolveSortBy(defaults)));
    }

    public String toUri(String basePath, UiPageDefaults defaults) {
        return UriComponentsBuilder.fromPath(basePath)
                .queryParam("page", resolvePage(defaults))
                .queryParam("size", resolveSize(defaults))
                .queryParam("sortBy", resolveSortBy(defaults))
                .queryParam("sortDirection", resolveSortDirection(defaults).name())
                .build()
                .encode()
                .toUriString();
    }

    public UiPageQuery applyDefaults(UiPageDefaults defaults) {
        UiPageQuery copy = copy();
        copy.setPage(resolvePage(defaults));
        copy.setSize(resolveSize(defaults));
        copy.setSortBy(resolveSortBy(defaults));
        copy.setSortDirection(resolveSortDirection(defaults));
        return copy;
    }

    public UiPageQuery copy() {
        UiPageQuery copy = new UiPageQuery();
        copy.setPage(page);
        copy.setSize(size);
        copy.setSortBy(sortBy);
        copy.setSortDirection(sortDirection);
        return copy;
    }

    protected boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    protected int resolvePage(UiPageDefaults defaults) {
        return page == null ? defaults.getPage() : page;
    }

    protected int resolveSize(UiPageDefaults defaults) {
        return size == null ? defaults.getSize() : size;
    }

    protected String resolveSortBy(UiPageDefaults defaults) {
        return isBlank(sortBy) ? defaults.getSortBy() : sortBy;
    }

    protected Sort.Direction resolveSortDirection(UiPageDefaults defaults) {
        return sortDirection == null ? defaults.getSortDirection() : sortDirection;
    }
}
