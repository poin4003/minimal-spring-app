package com.app.features.webhook.handler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class WebhookRuleFactory {

    private final Map<String, WebhookPartnerRule> rules;

    public WebhookRuleFactory(List<WebhookPartnerRule> ruleList) {
        this.rules = ruleList.stream()
                .collect(Collectors.toMap(WebhookPartnerRule::getPartnerCode, rule -> rule));
    }

    public WebhookPartnerRule getRule(String partnerCode) {
        return rules.getOrDefault(partnerCode, rules.get("DEFAULT"));
    }
}
