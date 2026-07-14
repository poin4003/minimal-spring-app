package com.app.features.rbac.web.view;

import java.util.UUID;

import com.app.core.schema.filter.BasePageFilter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionFilter extends BasePageFilter {

    private UUID roleId;
}
