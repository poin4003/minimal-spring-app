package com.app.features.post.catalogpost.web.view;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CatalogCategorySchemaForm {

    private List<@Valid CatalogCategoryAttributeAssignmentForm> assignments =
            new ArrayList<>();
}
