package com.app.features.rbac.cqrs.query;

import java.util.Objects;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.app.features.rbac.cqrs.result.PermissionResult;
import com.app.features.rbac.entity.PermissionEntity;
import com.app.features.rbac.filter.PermissionFilterCriteria;
import com.app.features.rbac.repository.PermissionRepository;
import com.app.features.rbac.repository.spec.PermissionSpecification;

import an.awesome.pipelinr.Command;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class GetManyPermissionsQuery implements Command<Page<PermissionResult>>, PermissionFilterCriteria {

    private UUID roleId;

    private Pageable pageable;
}

@Component
@RequiredArgsConstructor
class GetManyPermissionsHandler implements Command.Handler<GetManyPermissionsQuery, Page<PermissionResult>> {

    private final ModelMapper mapper;
    private final PermissionRepository permRepo;

    @Override
    public Page<PermissionResult> handle(GetManyPermissionsQuery query) {
        Pageable pageable = Objects.requireNonNull(query.getPageable());

        Specification<PermissionEntity> spec = PermissionSpecification.withFilter(query);

        Page<PermissionEntity> entityPage = permRepo.findAll(spec, pageable);

        return entityPage.map(result -> mapper.map(result, PermissionResult.class));
    }
}
