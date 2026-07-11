package com.app.features.sims.schema.result;

import java.util.UUID;

import com.app.features.sims.enums.SimStatusEnum;

import lombok.Data;

@Data
public class SimResult {
    private UUID id;

    private String phoneNumber;
    private SimStatusEnum status;
    private Integer sellingPrice;
    private Integer dealerPrice;
    private Integer importPrice;

    private String createdAt;
    private String updatedAt;
}
