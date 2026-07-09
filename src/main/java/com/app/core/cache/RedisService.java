package com.app.core.cache;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;

public interface RedisService {

    void setString(@NonNull String key, @Nullable String value);

    @Nullable
    String getString(@NonNull String key);

    void setObject(@NonNull String key, @Nullable Object value);

    @Nullable
    <T> T getObject(@NonNull String key, @NonNull Class<T> targetClass);

    void put(@NonNull String key, @NonNull Object value, long timeout, @NonNull TimeUnit unit);

    void put(@NonNull String key, @Nullable Object value, long expireTime);

    void delete(@NonNull String key);

    @NonNull
    RedisTemplate<String, Object> getRedisTemplate();

    void setInt(@NonNull String key, int value);

    int getInt(@NonNull String key);
}
