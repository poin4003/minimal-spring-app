package com.app.features.sims.excel;

import org.jobrunr.scheduling.JobScheduler;
import org.modelmapper.ModelMapper;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.app.features.sims.excel.dto.SimExcelImport;
import com.app.features.sims.schema.payload.CreateSimPayload;
import com.app.features.sims.worker.SimImportWorker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SimImportExcelListener extends AnalysisEventListener<SimExcelImport> {

    private final JobScheduler jobScheduler;
    private final SimImportWorker simImportWorker;
    private final ModelMapper mapper;

    @Override
    public void invoke(SimExcelImport data, AnalysisContext context) {
        log.info("Processing row: {}", context.readRowHolder().getRowIndex());

        CreateSimPayload payload = mapper.map(data, CreateSimPayload.class);

        jobScheduler.enqueue(() -> simImportWorker.execute(payload));
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("--- Excel import finished. All SIMs scheduled in JobRunr. ---");
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        log.error("Error reading Excel file at row {}: {}", context.readRowHolder().getRowIndex(),
                exception.getMessage());
        throw exception;
    }
}
