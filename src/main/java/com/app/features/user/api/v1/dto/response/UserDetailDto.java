package com.app.features.user.api.v1.dto.response;

import java.util.List;

import com.app.features.rbac.api.v1.dto.response.PermissionDto;
import com.app.features.rbac.api.v1.dto.response.RoleDto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserDetailDto extends UserDto {

    private List<RoleDto> roles;
    private List<PermissionDto> permissions;
}
