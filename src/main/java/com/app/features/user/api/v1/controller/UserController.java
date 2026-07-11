package com.app.features.user.api.v1.controller;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.app.core.constant.PermissionConstants;
import com.app.core.response.ApiResult;
import com.app.core.security.UserPrincipal;
import com.app.features.user.schema.payload.CreateUserPayload;
import com.app.features.user.schema.result.UserDetailResult;
import com.app.features.user.schema.result.UserResult;
import com.app.features.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
@Tag(name = "USER Management V1", description = "User docs")
public class UserController {

    private final UserService userSvc;
    private final ModelMapper mapper;

    @PostMapping("/")
    @Operation(summary = "Create new user")
    @ResponseStatus(HttpStatus.CREATED)
    @Secured(PermissionConstants.USER_CREATE)
    public ApiResult<UserResult> createUser(
            @Valid @RequestBody CreateUserPayload req) {
        UserResult result = userSvc.createUser(req);

        return ApiResult.ok(result, "Create user sucess!");
    }

    @GetMapping("/")
    @Operation(summary = "Get many user")
    @Secured(PermissionConstants.USER_VIEW)
    public ApiResult<Page<UserResult>> getManyUsers(
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserResult> results = userSvc.getManyUser(pageable);

        Page<UserResult> res = results.map(result -> mapper.map(result, UserResult.class));

        return ApiResult.ok(res, "Get many user success");
    }

    @GetMapping("/{userId}")
    @Secured(PermissionConstants.USER_VIEW)
    public ApiResult<UserDetailResult> getUserById(@PathVariable UUID userId) {
        UserDetailResult result = userSvc.getUserDetailById(userId);

        return ApiResult.ok(result, "Get user principal success!");
    }

    @GetMapping("/info")
    public ApiResult<UserDetailResult> getUserInfo(@AuthenticationPrincipal UserPrincipal currentUser) {
        UserDetailResult result = userSvc.getUserDetailById(currentUser.getUserId());

        return ApiResult.ok(mapper.map(result, UserDetailResult.class), "Get user principal success!");
    }
}
