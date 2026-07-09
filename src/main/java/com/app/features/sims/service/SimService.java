package com.app.features.sims.service;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.app.features.sims.excel.dto.SimExcelExport;
import com.app.features.sims.filter.SimFilterCriteria;

public interface SimService {

    List<SimExcelExport> getAllSimExcelExport(SimFilterCriteria criteria);

    void processExcelDataAnalytics() throws InterruptedException;

    void importSimsFromExcel(MultipartFile file) throws IOException;
}
