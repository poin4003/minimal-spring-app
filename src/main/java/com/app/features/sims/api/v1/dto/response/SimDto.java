package com.app.features.sims.api.v1.dto.response;

import java.util.UUID;

import com.app.features.sims.enums.SimStatusEnum;

import lombok.Data;

@Data
public class SimDto {
    private UUID id;

    private String phoneNumber;
    private SimStatusEnum status;
    private Integer sellingPrice;
    private Integer dealerPrice;
    private Integer importPrice;

    private String createdAt;
    private String updatedAt;
}
