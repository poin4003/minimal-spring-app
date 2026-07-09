package com.app.features.rbac.cqrs.query;

import java.util.Objects;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.app.features.rbac.cqrs.result.RoleResult;
import com.app.features.rbac.entity.RoleEntity;
import com.app.features.rbac.filter.RoleFilterCriteria;
import com.app.features.rbac.repository.RoleRepository;
import com.app.features.rbac.repository.spec.RoleSpecification;

import an.awesome.pipelinr.Command;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class GetManyRolesQuery implements Command<Page<RoleResult>>, RoleFilterCriteria {

    private UUID userId;

    private Pageable pageable;
}

@Component
@RequiredArgsConstructor
class GetManyRolesHandler implements Command.Handler<GetManyRolesQuery, Page<RoleResult>> {

    private final ModelMapper mapper;
    private final RoleRepository roleRepo;

    @Override
    public Page<RoleResult> handle(GetManyRolesQuery query) {
        Pageable pageable = Objects.requireNonNull(query.getPageable());

        Specification<RoleEntity> spec = RoleSpecification.withFilter(query);

        Page<RoleEntity> entityPage = roleRepo.findAll(spec, pageable);

        return entityPage.map(result -> mapper.map(result, RoleResult.class));
    }
}
