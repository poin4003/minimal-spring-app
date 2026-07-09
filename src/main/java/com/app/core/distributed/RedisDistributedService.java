package com.app.core.distributed;

public interface RedisDistributedService {
    RedisDistributedLocker getDistributedLock(String lockKey); 
}
