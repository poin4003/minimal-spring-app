package com.app.features.user.web.view;

import org.springframework.data.domain.Sort;

import com.app.core.schema.filter.BasePageFilter;
import com.app.features.ui.web.enums.UiAssignmentMode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRoleFilter extends BasePageFilter {

    private UiAssignmentMode mode = UiAssignmentMode.ASSIGNED;

    public UserRoleFilter() {
        setSortBy("key");
        setSortDirection(Sort.Direction.ASC);
    }
}
