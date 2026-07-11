package com.app.features.sims.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.app.features.sims.excel.dto.SimExcelExport;
import com.app.features.sims.schema.filter.SimFilterCriteria;
import com.app.features.sims.schema.payload.CreateSimPayload;
import com.app.features.sims.schema.result.SimResult;

public interface SimService {

    SimResult createSim(CreateSimPayload payload);

    Page<SimResult> getManySim(SimFilterCriteria criteria, Pageable pageable);

    SimResult getSimById(UUID simId);

    List<SimExcelExport> getAllSimExcelExport(SimFilterCriteria criteria);

    void importSimsFromExcel(MultipartFile file) throws IOException;
}
