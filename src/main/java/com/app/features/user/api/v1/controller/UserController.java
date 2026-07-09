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
import com.app.features.user.api.v1.dto.request.CreateUserDto;
import com.app.features.user.api.v1.dto.response.UserDetailDto;
import com.app.features.user.api.v1.dto.response.UserDto;
import com.app.features.user.cqrs.command.CreateUserCmd;
import com.app.features.user.cqrs.query.GetManyUserQuery;
import com.app.features.user.cqrs.query.GetUserByIdQuery;
import com.app.features.user.cqrs.result.UserDetailResult;
import com.app.features.user.cqrs.result.UserResult;

import an.awesome.pipelinr.Pipeline;
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

    private final ModelMapper mapper;
    private final Pipeline pipeline;

    @PostMapping("/")
    @Operation(summary = "Create new user")
    @ResponseStatus(HttpStatus.CREATED)
    @Secured(PermissionConstants.USER_CREATE)
    public ApiResult<UserDto> createUser(
            @Valid @RequestBody CreateUserDto req) {
        CreateUserCmd cmd = mapper.map(req, CreateUserCmd.class);

        UserResult result = pipeline.send(cmd);

        return ApiResult.ok(mapper.map(result, UserDto.class), "Create user sucess!");
    }

    @GetMapping("/")
    @Operation(summary = "Get many user")
    @Secured(PermissionConstants.USER_VIEW)
    public ApiResult<Page<UserDto>> getManyUsers(
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        GetManyUserQuery query = new GetManyUserQuery();

        query.setPageable(pageable);

        Page<UserResult> results = pipeline.send(query);

        Page<UserDto> res = results.map(result -> mapper.map(result, UserDto.class));

        return ApiResult.ok(res, "Get many user success");
    }

    @GetMapping("/{userId}")
    @Secured(PermissionConstants.USER_VIEW)
    public ApiResult<UserDetailDto> getUserById(@PathVariable UUID userId) {
        GetUserByIdQuery query = new GetUserByIdQuery(userId);

        UserDetailResult result = pipeline.send(query);

        return ApiResult.ok(mapper.map(result, UserDetailDto.class), "Get user principal success!");
    }

    @GetMapping("/info")
    public ApiResult<UserDetailDto> getUserInfo(@AuthenticationPrincipal UserPrincipal currentUser) {
        GetUserByIdQuery query = new GetUserByIdQuery(currentUser.getUserId());

        UserDetailResult result = pipeline.send(query);

        return ApiResult.ok(mapper.map(result, UserDetailDto.class), "Get user principal success!");
    }
}
