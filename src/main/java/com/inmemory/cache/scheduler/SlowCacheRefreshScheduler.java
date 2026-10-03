package com.inmemory.cache.scheduler;

import com.inmemory.cache.cache.SlowRefreshCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlowCacheRefreshScheduler {

    private final List<SlowRefreshCache> caches;

    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES)
    public void refresh() {

        for (SlowRefreshCache cache : caches) {

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
