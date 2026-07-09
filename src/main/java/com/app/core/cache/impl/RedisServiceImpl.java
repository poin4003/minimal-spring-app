package com.app.core.cache.impl;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.core.cache.RedisService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RedisServiceImpl implements RedisService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void setString(@NonNull String key, @Nullable String value) {
        if (value == null || !StringUtils.hasLength(value)) {
            return;
        }
        redisTemplate.opsForValue().set(key, value);
    } 

    @Override
    public String getString(@NonNull String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key))
                .map(String::valueOf)
                .orElse(null);
    }

    @Override
    public void setObject(@NonNull String key, @Nullable Object value) {
        if (value == null) { return; }

        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public <T> T getObject(@NonNull String key, @Nullable Class<T> targetClass) {
        Object result = redisTemplate.opsForValue().get(key);
        log.info("getObject result: {}", result);
        if (result == null) {
            return null;
        }

        if (result instanceof Map) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.convertValue(result, targetClass);
            } catch (IllegalArgumentException e) {
                log.error("Error converting LinkedHashMap to object: {}", e.getMessage());
                return null;
            }
        }

        if (result instanceof String) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue((String) result, targetClass);
            } catch (JsonProcessingException e) {
                log.error("Error deserializing JSON to object: {}", e.getMessage());
                return null;
            }
        }

        return null;
    }

    @Override
    public void put(@NonNull String key, @NonNull Object value, long timeout, @NonNull TimeUnit unit) {
        if (!StringUtils.hasLength(key) || value == null || unit == null) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.error("Error setting key '{}' with timeout: {}", key, e.getMessage());
        }
    }

    @Override
    public void put(@NonNull String key, @Nullable Object value, long expireTime) {
        if (!StringUtils.hasLength(key) || value == null) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error setting key '{}' with expireTime (seconds): {}", key, e.getMessage());
        }
    }

    @Override
    public void delete(@NonNull String key) {
        redisTemplate.delete(key);
    }

    @Override
    @NonNull
    public RedisTemplate<String, Object> getRedisTemplate() {
        return Objects.requireNonNull(redisTemplate, "redisTemplate must be injected");
    }

    @Override
    public void setInt(@NonNull String key, int value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public int getInt(@NonNull String key) {
        Object val = redisTemplate.opsForValue().get(key);

        if (val instanceof Integer) {
            return (Integer) val;
        }

        return 0;
    } 
}
