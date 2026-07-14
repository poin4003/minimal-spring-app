package com.app.features.rbac.web.view;

import java.util.UUID;

import com.app.core.schema.filter.BasePageFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleFilter extends BasePageFilter {

    private UUID userId;
}
