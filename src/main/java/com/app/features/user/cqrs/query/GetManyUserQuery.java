package com.app.features.user.cqrs.query;

import java.util.Objects;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.app.features.user.cqrs.result.UserResult;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.repository.UserBaseRepository;

import an.awesome.pipelinr.Command;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class GetManyUserQuery implements Command<Page<UserResult>> {

    private Pageable pageable;
}

@Component
@RequiredArgsConstructor
class GetManyUserHandler implements Command.Handler<GetManyUserQuery, Page<UserResult>> {

    private final UserBaseRepository userBaseRepo;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResult> handle(GetManyUserQuery query) {
        Pageable pageable = Objects.requireNonNull(query.getPageable());

        Page<UserBaseEntity> entityPage = userBaseRepo.findAll(pageable);

        return entityPage.map(result -> modelMapper.map(result, UserResult.class));
    }
}
