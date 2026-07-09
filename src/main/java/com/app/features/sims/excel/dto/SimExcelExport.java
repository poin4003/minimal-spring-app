package com.app.features.sims.excel.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

@Data
public class SimExcelExport {

    @ExcelProperty("ID Sim")
    private String id; 

    @ExcelProperty("Phone number")
    private String phoneNumber;
    
    @ExcelProperty("Status")
    private String status;

    @ExcelProperty("Selling price (VND)")
    private Integer sellingPrice;
    
    @ExcelProperty("Dealer price (VND)")
    private Integer dealerPrice;
    
    @ExcelProperty("Import price (VND)")
    private Integer importPrice;

    @ExcelProperty("Created at")
    @DateTimeFormat("dd-MM-yyyy HH:mm:ss")
    private String createdAt;

    @ExcelProperty("Updated at")
    @DateTimeFormat("dd-MM-yyyy HH:mm:ss")
    private String updatedAt;
}
