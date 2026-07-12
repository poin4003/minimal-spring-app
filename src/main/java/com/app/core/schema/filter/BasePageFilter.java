package com.app.core.schema.filter;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BasePageFilter {

    @Min(value = 0, message = "Page must be greater than or equal to 0")
    private Integer page = 0;

    @Min(value = 1, message = "Size must be greater than or equal to 1")
    @Max(value = 100, message = "Size must be less than or equal to 100")
    private Integer size = 10;

    private String sortBy = "createdAt";

    private Sort.Direction sortDirection = Sort.Direction.DESC;

    public Pageable toPageable() {
        return PageRequest.of(
                page == null ? 0 : page,
                size == null ? 10 : size,
                Sort.by(
                        sortDirection == null ? Sort.Direction.DESC : sortDirection,
                        (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy));
    }
}
