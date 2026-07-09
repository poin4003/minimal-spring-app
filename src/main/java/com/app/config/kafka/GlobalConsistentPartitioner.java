package com.app.config.kafka;

import java.util.Map;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.utils.Utils;

public class GlobalConsistentPartitioner implements Partitioner {

    private static final int VIRTUAL_SHARDS = 1024;

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        if (keyBytes == null)
            return 0;

        int numPartitions = cluster.partitionsForTopic(topic).size();

        int shard = Utils.toPositive(Utils.murmur2(keyBytes)) % VIRTUAL_SHARDS;

        return (int) ((long) shard * numPartitions / VIRTUAL_SHARDS);
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}
