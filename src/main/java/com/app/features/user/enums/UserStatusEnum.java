package com.app.features.user.enums;

import com.app.core.annotation.DatabaseEnum;
import com.app.core.base.BaseEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@DatabaseEnum
public enum UserStatusEnum implements BaseEnum {
    ACTIVE(1, "Active"),
    INACTIVE(2, "Inactive"),
    LOCKED(3, "Locked")
    ;

    private final int code;
    private final String description;
}
