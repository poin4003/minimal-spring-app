package com.app.config.kafka.dlq;

public interface DlqTopicResolver {

    String resolve(String topic, String group);

}
