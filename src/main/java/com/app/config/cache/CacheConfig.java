package com.app.config.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.app.config.settings.AppProperties;
import com.app.core.constant.CacheConstants;
import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    CacheManager cacheManager(AppProperties appProperties) {
        AppProperties.KeyStoreCache settings = appProperties.getCache().getKeyStore();

        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setAllowNullValues(false);
        caffeineCacheManager.registerCustomCache(
                CacheConstants.KEY_STORE,
                Caffeine.newBuilder()
                        .maximumSize(settings.getMaximumSize())
                        .expireAfterWrite(settings.getTtl())
                        .build());

        return new TransactionAwareCacheManagerProxy(caffeineCacheManager);
    }
}
