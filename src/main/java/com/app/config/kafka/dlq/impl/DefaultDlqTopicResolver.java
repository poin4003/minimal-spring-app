package com.app.config.kafka.dlq.impl;

import org.springframework.stereotype.Component;

import com.app.config.kafka.dlq.DlqTopicResolver;

@Component
public class DefaultDlqTopicResolver implements DlqTopicResolver {
    
    @Override
    public String resolve(String topic, String group) {
        return topic + "." + group + ".DLQ";
    }
}
