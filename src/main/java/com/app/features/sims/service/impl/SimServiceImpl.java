package com.app.features.sims.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jobrunr.scheduling.JobScheduler;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.app.core.exception.ExceptionFactory;
import com.app.features.sims.entity.SimEntity;
import com.app.features.sims.excel.SimImportExcelListener;
import com.app.features.sims.excel.dto.SimExcelExport;
import com.app.features.sims.excel.dto.SimExcelImport;
import com.app.features.sims.repository.SimRepsitory;
import com.app.features.sims.repository.spec.SimSpecification;
import com.app.features.sims.schema.filter.SimFilterCriteria;
import com.app.features.sims.schema.payload.CreateSimPayload;
import com.app.features.sims.schema.result.SimResult;
import com.app.features.sims.service.SimService;
import com.app.features.sims.worker.SimImportWorker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimServiceImpl implements SimService {

    private final SimRepsitory simRepo;
    private final JobScheduler jobScheduler;
    private final SimImportWorker simImportWorker;
    private final ModelMapper mapper;

    @Override
    public SimResult createSim(CreateSimPayload payload) {
        if (simRepo.existsByPhoneNumber(payload.getPhoneNumber())) {
            throw ExceptionFactory.alreadyExists("PhoneNumber " + payload.getPhoneNumber());
        }

        SimEntity sim = new SimEntity();
        sim.setPhoneNumber(payload.getPhoneNumber());
        sim.setImportPrice(payload.getImportPrice());
        sim.setSellingPrice(payload.getSellingPrice());
        sim.setDealerPrice(payload.getDealerPrice());
        sim.setStatus(payload.getStatus());

        simRepo.save(sim);

        return mapper.map(sim, SimResult.class);
    }

    @Override
    public Page<SimResult> getManySim(SimFilterCriteria criteria, Pageable pageable) {
        Specification<SimEntity> spec = SimSpecification.withFilter(criteria);

        Page<SimEntity> entityPage = simRepo.findAll(spec, pageable);

        return entityPage.map(result -> mapper.map(result, SimResult.class));
    }

    @Override
    public SimResult getSimById(UUID simId) {
        SimEntity sim = simRepo.findById(simId)
                .orElseThrow(() -> ExceptionFactory.notFound("Sim: " + simId));

        return mapper.map(sim, SimResult.class);
    }

    @Override
    public void importSimsFromExcel(MultipartFile file) throws IOException {
        SimImportExcelListener listener = new SimImportExcelListener(jobScheduler, simImportWorker, mapper);

        log.info("Starting to read Excel file: {}", file.getOriginalFilename());

        try {
            EasyExcel.read(
                    file.getInputStream(),
                    SimExcelImport.class,
                    listener).sheet().doRead();

        } catch (IOException e) {
            log.error("Error reading input stream for Excel file: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public List<SimExcelExport> getAllSimExcelExport(SimFilterCriteria criteria) {
        Specification<SimEntity> spec = SimSpecification.withFilter(criteria);

        List<SimEntity> allSimEntities = simRepo.findAll(spec);

        List<SimExcelExport> allSimResponses = allSimEntities.stream()
                .map(entity -> mapper.map(entity, SimExcelExport.class))
                .collect(Collectors.toList());

        return allSimResponses;
    }
}
