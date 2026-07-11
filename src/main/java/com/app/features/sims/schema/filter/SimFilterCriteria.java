package com.app.features.sims.schema.filter;

import java.time.LocalDateTime;

import com.app.features.sims.enums.SimStatusEnum;

import lombok.Data;

@Data
public class SimFilterCriteria {

    private String phoneNumber;

    private SimStatusEnum status;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;
}
