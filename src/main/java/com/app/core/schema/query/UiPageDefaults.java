package com.app.core.schema.query;

import org.springframework.data.domain.Sort;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UiPageDefaults {

    private final int page;
    private final int size;
    private final String sortBy;
    private final Sort.Direction sortDirection;
}
