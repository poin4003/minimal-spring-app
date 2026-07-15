package com.app.features.user.schema.payload;

import com.app.features.user.enums.UserStatusEnum;

import lombok.Data;

@Data
public class UpdateUserPayload {

    private UserStatusEnum status;
}
