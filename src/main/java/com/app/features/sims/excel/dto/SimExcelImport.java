package com.app.features.sims.excel.dto;

import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

@Data
public class SimExcelImport {

    @ExcelProperty(value = "phone_number")
    private String phoneNumber;

    @ExcelProperty(value = "selling_price")
    private Integer sellingPrice;

    @ExcelProperty(value = "dealer_price")
    private Integer dealerPrice;

    @ExcelProperty(value = "import_price")
    private Integer importPrice;
    
    @ExcelProperty(value = "status")
    private String status;
}
