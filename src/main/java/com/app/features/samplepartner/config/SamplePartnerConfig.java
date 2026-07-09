package com.app.features.samplepartner.config;

import com.app.core.constant.PartnerConstants;
import com.app.features.webhook.annotation.WebhookPartner;

import lombok.Data;

@Data
@WebhookPartner(code = PartnerConstants.SAMPLE_PARTNER)
public class SamplePartnerConfig {

    private String username = ""; 

    private String password = "";
}
