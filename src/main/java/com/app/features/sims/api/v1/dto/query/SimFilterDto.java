package com.app.features.sims.api.v1.dto.query;

import java.time.LocalDateTime;

import com.app.features.sims.enums.SimStatusEnum;
import com.app.features.sims.filter.SimFilterCriteria;

import lombok.Data;

@Data
public class SimFilterDto implements SimFilterCriteria {

    private String phoneNumber;

    private SimStatusEnum status;

    private LocalDateTime fromDate;
    
    private LocalDateTime toDate;
}
