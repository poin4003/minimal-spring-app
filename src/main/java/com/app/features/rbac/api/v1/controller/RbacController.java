package com.app.features.rbac.api.v1.controller;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.app.core.constant.PermissionConstants;
import com.app.core.response.ApiResult;
import com.app.features.rbac.api.v1.dto.query.PermissionFilterDto;
import com.app.features.rbac.api.v1.dto.query.RoleFilterDto;
import com.app.features.rbac.api.v1.dto.request.CreateRoleDto;
import com.app.features.rbac.api.v1.dto.request.RolePermissionsDto;
import com.app.features.rbac.api.v1.dto.request.UpdateRoleDto;
import com.app.features.rbac.api.v1.dto.request.UserRolesDto;
import com.app.features.rbac.api.v1.dto.response.PermissionDto;
import com.app.features.rbac.api.v1.dto.response.RoleDto;
import com.app.features.rbac.cqrs.command.AssignPermToRoleCmd;
import com.app.features.rbac.cqrs.command.AssignRoleToUserCmd;
import com.app.features.rbac.cqrs.command.CreateRoleCmd;
import com.app.features.rbac.cqrs.command.DeleteRoleCmd;
import com.app.features.rbac.cqrs.command.RemovePermFromRoleCmd;
import com.app.features.rbac.cqrs.command.RemoveRoleFromUserCmd;
import com.app.features.rbac.cqrs.command.UpdateRoleCmd;
import com.app.features.rbac.cqrs.query.GetManyPermissionsQuery;
import com.app.features.rbac.cqrs.query.GetManyRolesQuery;
import com.app.features.rbac.cqrs.result.PermissionResult;
import com.app.features.rbac.cqrs.result.RoleResult;

import an.awesome.pipelinr.Pipeline;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@RequestMapping("/api/v1/rbac")
@Secured(PermissionConstants.RBAC_MANAGE)
@Tag(name = "RBAC Management V1", description = "RBAC docs")
public class RbacController {

    private final ModelMapper mapper;
    private final Pipeline pipeline;

    @PostMapping("/role")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<RoleDto> createRole(@Valid @RequestBody CreateRoleDto req) {
        RoleResult result = pipeline.send(new CreateRoleCmd(req.getName(), req.getKey()));

        return ApiResult.ok(mapper.map(result, RoleDto.class), "Create role success!");
    }

    @PatchMapping("/role/{id}")
    public ApiResult<RoleDto> updateRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody UpdateRoleDto req) {
        RoleResult result = pipeline.send(new UpdateRoleCmd(roleId, req.getName(), req.getKey()));

        return ApiResult.ok(mapper.map(result, RoleDto.class), "Update role sucess!");
    }

    @DeleteMapping("/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResult<Void> deleteRole(
            @PathVariable UUID roleId) {
        pipeline.send(new DeleteRoleCmd(roleId));

        return ApiResult.ok(null, "Delete role success!");
    }

    @GetMapping("/role")
    public ApiResult<Page<RoleDto>> getManyRoles(
            @ParameterObject RoleFilterDto req,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        GetManyRolesQuery query = mapper.map(req, GetManyRolesQuery.class);

        query.setPageable(pageable);

        Page<RoleResult> results = pipeline.send(query);

        Page<RoleDto> res = results.map(result -> mapper.map(result, RoleDto.class));

        return ApiResult.ok(res, "Get many role success!");
    }

    @GetMapping("/permisision")
    public ApiResult<Page<PermissionDto>> getManyPermissions(
            @ParameterObject PermissionFilterDto req,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        GetManyPermissionsQuery query = mapper.map(req, GetManyPermissionsQuery.class);

        query.setPageable(pageable);

        Page<PermissionResult> results = pipeline.send(query);

        Page<PermissionDto> res = results.map(result -> mapper.map(result, PermissionDto.class));

        return ApiResult.ok(res, "Get many permission success!");
    }

    @PostMapping("/assign-roles")
    public ApiResult<Void> assignRolesToUser(@Valid @RequestBody UserRolesDto req) {
        pipeline.send(new AssignRoleToUserCmd(req.getUserId(), req.getRoleIds()));

        return ApiResult.ok(null, "Assign roles to user success!");
    }

    @PostMapping("/remove-roles")
    public ApiResult<Void> removeRolesFromUser(@Valid @RequestBody UserRolesDto req) {
        pipeline.send(new RemoveRoleFromUserCmd(req.getUserId(), req.getRoleIds()));

        return ApiResult.ok(null, "Remove roles from user success!");
    }

    @PostMapping("assign-permissions")
    public ApiResult<Void> assignPermissionsToRole(@Valid @RequestBody RolePermissionsDto req) {
        pipeline.send(new AssignPermToRoleCmd(req.getRoleId(), req.getPermIds()));

        return ApiResult.ok(null, "Assign permission to role success!");
    }

    @PostMapping("remove-permissions")
    public ApiResult<Void> removePermissionsFromRole(@Valid @RequestBody RolePermissionsDto req) {
        pipeline.send(new RemovePermFromRoleCmd(req.getRoleId(), req.getPermIds()));

        return ApiResult.ok(null, "Remove permissions from role success!");
    }
}
