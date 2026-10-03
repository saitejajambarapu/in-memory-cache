package com.inmemory.cache.scheduler;

import com.inmemory.cache.cache.FastRefreshCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class FastCacheRefreshScheduler {

    private final List<FastRefreshCache> caches;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void refresh() {

        for (FastRefreshCache cache : caches) {

            try {
                cache.refreshData();
            } catch (Exception e) {
                log.error(
                        "Failed to refresh cache: {}",
                        cache.getClass().getSimpleName(),
                        e
                );
            }
        }
    }
}