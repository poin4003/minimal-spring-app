package com.app.core.distributed.impl;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import com.app.core.distributed.RedisDistributedLocker;
import com.app.core.distributed.RedisDistributedService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RedisDistributedLockerImpl implements RedisDistributedService {
    @Resource
    private RedissonClient redissonClient;

    @Override
    public RedisDistributedLocker getDistributedLock(String lockKey) {
        RLock rlock = redissonClient.getLock(lockKey);

        return new RedisDistributedLocker() {
            @Override
            public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
                boolean isLockSuccess = rlock.tryLock(waitTime, leaseTime, unit);
                log.info("{} get lock result: {}", lockKey, isLockSuccess);
                return isLockSuccess;
            }

            @Override
            public void lock(long leaseTime, TimeUnit unit) {
                rlock.lock(leaseTime, unit);
            }

            @Override
            public void unlock() {
                if (isLocked() && isHeldByCurrentThread()) {
                    rlock.unlock();
                }
            }

            @Override
            public boolean isLocked() {
                return rlock.isLocked();
            }

            @Override
            public boolean isHeldByThread(long threadId) {
                return rlock.isHeldByThread(threadId);
            }

            @Override
            public boolean isHeldByCurrentThread() {
                return rlock.isHeldByCurrentThread();
            }
        };
    } 
}
