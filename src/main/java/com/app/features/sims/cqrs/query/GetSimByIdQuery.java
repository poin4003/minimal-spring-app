package com.app.features.sims.cqrs.query;

import java.util.Objects;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.app.core.exception.ExceptionFactory;
import com.app.features.sims.cqrs.result.SimResult;
import com.app.features.sims.entity.SimEntity;
import com.app.features.sims.repository.SimRepsitory;

import an.awesome.pipelinr.Command;
import lombok.RequiredArgsConstructor;

public record GetSimByIdQuery(UUID Id) implements Command<SimResult> {
}

@Component
@RequiredArgsConstructor
class GetSimByIdHandler implements Command.Handler<GetSimByIdQuery, SimResult> {

    private final SimRepsitory simRepo;
    private final ModelMapper mapper;

    @Override
    public SimResult handle(GetSimByIdQuery query) {
        UUID id = Objects.requireNonNull(query.Id(), "Sim Id must be not null");

        SimEntity sim = simRepo.findById(id)
                .orElseThrow(() -> ExceptionFactory.notFound("Sim: " + query.Id()));

        return mapper.map(sim, SimResult.class);
    }
}
