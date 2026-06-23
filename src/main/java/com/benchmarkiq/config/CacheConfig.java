package com.benchmarkiq.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_TEST_CONFIGS = "testConfigs";
    public static final String CACHE_DASHBOARD = "dashboard";
    public static final String CACHE_TEST_RESULTS = "testResults";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                CACHE_TEST_CONFIGS,
                CACHE_DASHBOARD,
                CACHE_TEST_RESULTS
        );
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());
        return manager;
    }
}
