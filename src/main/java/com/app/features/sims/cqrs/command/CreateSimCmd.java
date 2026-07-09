package com.app.features.sims.cqrs.command;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.app.core.annotation.RegisterMqType;
import com.app.core.exception.ExceptionFactory;
import com.app.features.sims.cqrs.result.SimResult;
import com.app.features.sims.entity.SimEntity;
import com.app.features.sims.enums.SimStatusEnum;
import com.app.features.sims.repository.SimRepsitory;

import an.awesome.pipelinr.Command;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RegisterMqType("CreateSimCmd")
public class CreateSimCmd implements Command<SimResult> {
    private String phoneNumber;
    private Integer importPrice;
    private Integer sellingPrice;
    private Integer dealerPrice;
    private SimStatusEnum status;
}

@Component
@RequiredArgsConstructor
class CreateSimHandler implements Command.Handler<CreateSimCmd, SimResult> {

    private final SimRepsitory simRepo;
    private final ModelMapper mapper;

    @Override
    public SimResult handle(CreateSimCmd cmd) {
        if (simRepo.existsByPhoneNumber(cmd.getPhoneNumber())) {
            throw ExceptionFactory.alreadyExists("PhoneNumber " + cmd.getPhoneNumber());
        }

        SimEntity sim = new SimEntity();
        sim.setPhoneNumber(cmd.getPhoneNumber());
        sim.setImportPrice(cmd.getImportPrice());
        sim.setSellingPrice(cmd.getSellingPrice());
        sim.setDealerPrice(cmd.getDealerPrice());
        sim.setStatus(cmd.getStatus());

        simRepo.save(sim);

        return mapper.map(sim, SimResult.class);
    }
}
