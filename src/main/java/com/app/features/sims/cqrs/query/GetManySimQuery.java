package com.app.features.sims.cqrs.query;

import java.time.LocalDateTime;
import java.util.Objects;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.app.features.sims.cqrs.result.SimResult;
import com.app.features.sims.entity.SimEntity;
import com.app.features.sims.enums.SimStatusEnum;
import com.app.features.sims.filter.SimFilterCriteria;
import com.app.features.sims.repository.SimRepsitory;
import com.app.features.sims.repository.spec.SimSpecification;

import an.awesome.pipelinr.Command;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class GetManySimQuery implements Command<Page<SimResult>>, SimFilterCriteria {

    private String phoneNumber;

    private SimStatusEnum status;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

    private Pageable pageable;
}

@Component
@RequiredArgsConstructor
class GetManySimHandler implements Command.Handler<GetManySimQuery, Page<SimResult>> {

    private final SimRepsitory simRepo;
    private final ModelMapper mapper;

    @Override
    public Page<SimResult> handle(GetManySimQuery query) {
        Pageable pageable = Objects.requireNonNull(query.getPageable());

        Specification<SimEntity> spec = SimSpecification.withFilter(query);

        Page<SimEntity> entityPage = simRepo.findAll(spec, pageable);

        return entityPage.map(result -> mapper.map(result, SimResult.class));
    }
}
