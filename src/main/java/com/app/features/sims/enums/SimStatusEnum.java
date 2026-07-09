package com.app.features.sims.enums;

import com.app.core.annotation.DatabaseEnum;
import com.app.core.base.BaseEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@DatabaseEnum
public enum SimStatusEnum implements BaseEnum {
    ACTIVE(1, "Active"),
    INACTIVE(2, "Inactive"),
    BLOCKED(3, "Blocked"),
    PICKED(4, "Picked"),
    DELETED(5, "Deleted");

    private final int code;
    private final String description;
}
