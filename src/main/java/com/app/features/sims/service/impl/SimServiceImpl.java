package com.app.features.sims.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.app.core.utils.TaskRunnerUtils;
import com.app.core.utils.ThreadPoolUtils;
import com.app.features.sims.entity.SimEntity;
import com.app.features.sims.excel.SimImportExcelListener;
import com.app.features.sims.excel.dto.SimExcelExport;
import com.app.features.sims.excel.dto.SimExcelImport;
import com.app.features.sims.filter.SimFilterCriteria;
import com.app.features.sims.producer.SimImportProducer;
import com.app.features.sims.repository.SimRepsitory;
import com.app.features.sims.repository.spec.SimSpecification;
import com.app.features.sims.service.SimService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimServiceImpl implements SimService {

    private final SimRepsitory simRepo;
    private final SimImportProducer simImportProducer;
    private final ModelMapper modelMapper;

    @Override
    public void importSimsFromExcel(MultipartFile file) throws IOException {
        SimImportExcelListener listener = new SimImportExcelListener(simImportProducer, modelMapper);

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
                .map(entity -> modelMapper.map(entity, SimExcelExport.class))
                .collect(Collectors.toList());

        return allSimResponses;
    }

    @Override
    public void processExcelDataAnalytics() throws InterruptedException {
        int totalCount = 999999;
        int chunkRows = 5000;
        int chunkLoop = totalCount / chunkRows + (totalCount % chunkRows == 0 ? 0 : 1);
        int ccuPoolRate = 3;
        int threadLoop = chunkLoop / ccuPoolRate + (chunkLoop % ccuPoolRate == 0 ? 0 : 1);

        ThreadPoolTaskExecutor threadPoolTaskExecutor = ThreadPoolUtils.createThreadPoolTaskExecutor(9, 12,
                threadLoop * ccuPoolRate, 60, "thread-excel-");

        try {
            TaskRunnerUtils.runInParallelBatches(totalCount, ccuPoolRate, threadLoop, threadPoolTaskExecutor,
                    (startRows, endRows) -> {
                        System.out.println("Processing: startRows " + startRows + " | endRows " + endRows);
                    });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            threadPoolTaskExecutor.shutdown();
        }
    }
}
