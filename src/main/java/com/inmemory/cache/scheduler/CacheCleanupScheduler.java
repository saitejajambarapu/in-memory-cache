package com.inmemory.cache.scheduler;

import com.inmemory.cache.cache.CacheMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheCleanupScheduler {

    private final List<CacheMemory> caches;

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void cleanup() {

        for (CacheMemory cache : caches) {

            try {
                cache.cleanUp();
            } catch (Exception e) {
                log.error(
                        "Failed to cleanup cache: {}",
                        cache.getClass().getSimpleName(),
                        e
                );
            }
        }
    }
}
