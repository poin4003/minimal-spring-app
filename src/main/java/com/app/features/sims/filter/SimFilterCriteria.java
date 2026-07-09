package com.app.features.sims.filter;

import java.time.LocalDateTime;

import com.app.features.sims.enums.SimStatusEnum;

public interface SimFilterCriteria {
    String getPhoneNumber();

    SimStatusEnum getStatus();

    LocalDateTime getFromDate();

    LocalDateTime getToDate();
}
