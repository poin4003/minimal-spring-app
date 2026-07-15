package com.app.features.cronjob.schema.result;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CronJobDetailResult extends CronJobResult {

    private String name;

    private String defaultCron;

    private String effectiveCron;

    private String zoneId;

    private boolean usingDefaultCron;
}
