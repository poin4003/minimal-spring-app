package com.app.features.webhook.sync;

import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.core.sync.SyncableDataService;
import com.app.core.utils.JsonNodeConverter;
import com.app.features.webhook.annotation.WebhookPartner;
import com.app.features.webhook.entity.WebhookSubscriptionEntity;
import com.app.features.webhook.enums.WebhookSubscriptionStatus;
import com.app.features.webhook.repository.WebhookSubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookSyncService implements SyncableDataService {

    private final ApplicationContext applicationContext;
    private final WebhookSubscriptionRepository subscriptionRepo;
    private final JsonNodeConverter jsonConverter;

    @Override
    public String getSyncType() {
        return "SYNC_WEBHOOK_PLUGINS";
    }

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncToDatabase() {
        log.info("[Sync] Scanning for webhook partner plugins...");
        int processedCount = 0;

        Map<String, Object> partnerConfigs = applicationContext.getBeansWithAnnotation(WebhookPartner.class);

        for (Object configInstance : partnerConfigs.values()) {
            WebhookPartner annotation = configInstance.getClass().getAnnotation(WebhookPartner.class);

            String partnerCode = annotation.code();

            try {
                JsonNode templateNode = jsonConverter.toNode(configInstance);

                WebhookSubscriptionEntity sub = subscriptionRepo.findByPartnerCode(partnerCode).orElse(null);

                if (sub == null) {
                    sub = new WebhookSubscriptionEntity();
                    sub.setPartnerCode(partnerCode);

                    sub.setStatus(WebhookSubscriptionStatus.INACTIVE);
                    sub.setUrl("");

                    sub.setMaxRpm(60);
                    sub.setBaseDelaySeconds(30);
                    sub.setMaxDelaySeconds(3600);
                    sub.setMaxRetries(5);

                    // TODO: replace with real partner
                    sub.setPartnerId(UUID.randomUUID());
                    sub.setConfig(jsonConverter.toNode(configInstance));

                    log.debug("[Sync] Inserted new Webhook Plugin config for: [{}]", partnerCode);
                } else {
                    if (templateNode.isObject()) {
                        ObjectNode finalConfig = ((ObjectNode) templateNode).deepCopy();

                        JsonNode dbConfig = sub.getConfig();

                        if (dbConfig != null && dbConfig.isObject()) {
                            finalConfig.fieldNames().forEachRemaining(key -> {
                                if (dbConfig.hasNonNull(key)) {
                                    finalConfig.set(key, dbConfig.get(key));
                                }
                            });
                        }
                        sub.setConfig(finalConfig);
                    }
                    log.debug("[Sync] Updated exiting Webhook Plugin config for: [{}]", partnerCode);
                }

                subscriptionRepo.save(sub);
                processedCount++;

            } catch (Exception e) {
                log.error("[Sync] Error processing config for partner: {}", partnerCode, e);
            }
        }

        log.info(">>> Sync [{}] COMPLETE. Processed plugins: {}", getSyncType(), processedCount);
    }
}
