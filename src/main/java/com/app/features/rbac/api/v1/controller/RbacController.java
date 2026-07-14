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
import com.app.features.rbac.schema.filter.PermissionFilterCriteria;
import com.app.features.rbac.schema.filter.RoleFilterCriteria;
import com.app.features.rbac.schema.payload.CreateRolePayload;
import com.app.features.rbac.schema.payload.RolePermissionsPayload;
import com.app.features.rbac.schema.payload.UpdateRolePayload;
import com.app.features.rbac.schema.payload.UserRolesPayload;
import com.app.features.rbac.schema.result.PermissionResult;
import com.app.features.rbac.schema.result.RoleResult;
import com.app.features.rbac.service.RbacService;

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

    private final RbacService rbacSvc;
    private final ModelMapper mapper;

    @PostMapping("/role")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<RoleResult> createRole(@Valid @RequestBody CreateRolePayload req) {
        RoleResult result = rbacSvc.createRole(req);

        return ApiResult.ok(result, "Create role success!");
    }

    @PatchMapping("/role/{roleId}")
    public ApiResult<RoleResult> updateRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody UpdateRolePayload req) {
        RoleResult result = rbacSvc.updateRole(roleId, req);

        return ApiResult.ok(result, "Update role sucess!");
    }

    @DeleteMapping("/role/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResult<Void> deleteRole(
            @PathVariable UUID roleId) {
        rbacSvc.deleteRole(roleId);

        return ApiResult.ok(null, "Delete role success!");
    }

    @GetMapping("/role")
    public ApiResult<Page<RoleResult>> getManyRoles(
            @ParameterObject RoleFilterCriteria req,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<RoleResult> results = rbacSvc.getManyRoles(req, pageable);

        Page<RoleResult> res = results.map(result -> mapper.map(result, RoleResult.class));

        return ApiResult.ok(res, "Get many role success!");
    }

    @GetMapping("/permisision")
    public ApiResult<Page<PermissionResult>> getManyPermissions(
            @ParameterObject PermissionFilterCriteria req,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PermissionResult> results = rbacSvc.getManyPermissions(req, pageable);

        Page<PermissionResult> res = results.map(result -> mapper.map(result, PermissionResult.class));

        return ApiResult.ok(res, "Get many permission success!");
    }

    @PostMapping("/assign-roles")
    public ApiResult<Void> assignRolesToUser(@Valid @RequestBody UserRolesPayload req) {
        rbacSvc.assignRoleToUser(req.getUserId(), req.getRoleIds());

        return ApiResult.ok(null, "Assign roles to user success!");
    }

    @PostMapping("/remove-roles")
    public ApiResult<Void> removeRolesFromUser(@Valid @RequestBody UserRolesPayload req) {
        rbacSvc.removeRoleFromUser(req.getUserId(), req.getRoleIds());

        return ApiResult.ok(null, "Remove roles from user success!");
    }

    @PostMapping("/assign-permissions")
    public ApiResult<Void> assignPermissionsToRole(@Valid @RequestBody RolePermissionsPayload req) {
        rbacSvc.assignPermToRole(req.getRoleId(), req.getPermIds());

        return ApiResult.ok(null, "Assign permission to role success!");
    }

    @PostMapping("/remove-permissions")
    public ApiResult<Void> removePermissionsFromRole(@Valid @RequestBody RolePermissionsPayload req) {
        rbacSvc.removePermFromRole(req.getRoleId(), req.getPermIds());

        return ApiResult.ok(null, "Remove permissions from role success!");
    }
}
