package com.app.features.ui.web.query;

import org.springframework.web.util.UriComponentsBuilder;

import com.app.core.schema.query.UiPageDefaults;
import com.app.core.schema.query.UiPageQuery;
import com.app.features.ui.web.enums.UiAssignmentMode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UiAssignmentPageQuery extends UiPageQuery {

    private UiAssignmentMode mode = UiAssignmentMode.ASSIGNED;

    @Override
    public String toUri(String basePath, UiPageDefaults defaults) {
        return UriComponentsBuilder.fromUriString(super.toUri(basePath, defaults))
                .queryParam("mode", mode.name())
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public UiAssignmentPageQuery copy() {
        UiAssignmentPageQuery copy = new UiAssignmentPageQuery();
        copy.setPage(getPage());
        copy.setSize(getSize());
        copy.setSortBy(getSortBy());
        copy.setSortDirection(getSortDirection());
        copy.setMode(mode);
        return copy;
    }

    @Override
    public UiAssignmentPageQuery applyDefaults(UiPageDefaults defaults) {
        UiAssignmentPageQuery copy = copy();
        copy.setPage(resolvePage(defaults));
        copy.setSize(resolveSize(defaults));
        copy.setSortBy(resolveSortBy(defaults));
        copy.setSortDirection(resolveSortDirection(defaults));
        copy.setMode(mode == null ? UiAssignmentMode.ASSIGNED : mode);
        return copy;
    }

    public UiAssignmentPageQuery forMode(UiAssignmentMode nextMode) {
        UiAssignmentPageQuery copy = copy();
        copy.setMode(nextMode);
        copy.setPage(0);
        return copy;
    }
}
