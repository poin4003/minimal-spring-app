package com.app.features.sims.excel;

import org.modelmapper.ModelMapper;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.app.features.sims.producer.SimImportProducer;
import com.app.features.sims.cqrs.command.CreateSimCmd;
import com.app.features.sims.excel.dto.SimExcelImport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SimImportExcelListener extends AnalysisEventListener<SimExcelImport> {
    
    private final SimImportProducer simImportProducer;
    private final ModelMapper modelMapper;

    @Override
    public void invoke(SimExcelImport data, AnalysisContext context) {
        log.info("Processing row: {}", context.readRowHolder().getRowIndex());

        CreateSimCmd cmd = modelMapper.map(data, CreateSimCmd.class);

        simImportProducer.sendSimToImportQueue(cmd);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("--- Excel import finished. All SIMs sent to Kafka queue. ---");
    }
    
    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        log.error("Error reading Excel file at row {}: {}", context.readRowHolder().getRowIndex(), exception.getMessage());
        throw exception; 
    }
}
