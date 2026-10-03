package com.inmemory.cache.cache;

import com.inmemory.cache.dto.CacheDto;
import com.inmemory.cache.model.Department;
import com.inmemory.cache.record.DepartmentCacheKey;
import com.inmemory.cache.repositories.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("AllDepartmentsCache")
@Slf4j
@RequiredArgsConstructor
public class AllDepartmentsCache
        implements CacheMemory, CacheHandler,SlowRefreshCache {

    private final DepartmentRepository departmentRepository;

    /*
     * (name, location) -> department list
     */
    private final Map<
            DepartmentCacheKey,
            CacheDto<List<Department>>
            > memory = new ConcurrentHashMap<>();

    private LocalDateTime lastUpdatedAt =
            LocalDateTime.now();

    private final Long CACHE_TTL = 1L;

    private final Long CACHE_REFRESH_TIME_MINUTES = 5L;

    private final Long CACHE_EVICT_TIME_MINUTES = 10L;



    /*
     * Called by AOP during cache lookup
     */
    @Override
    public Object get(Object[] args) {

        String name = (String) args[0];
        String location = (String) args[1];

        DepartmentCacheKey key =
                new DepartmentCacheKey(
                        name,
                        location
                );

        CacheDto<List<Department>> cache =
                memory.get(key);

        if (cache == null) {
            return null;
        }

        if (!checkTTL(
                cache.getAddedAt(),
                CACHE_TTL)) {

            memory.remove(key);

            return null;
        }

        // addedAt = last accessed time
        cache.setAddedAt(LocalDateTime.now());

        return cache.getData();
    }


    /*
     * Called by AOP after DB call
     */
    @Override
    @SuppressWarnings("unchecked")
    public void put(
            Object[] args,
            Object data) {

        String name = (String) args[0];
        String location = (String) args[1];

        DepartmentCacheKey key =
                new DepartmentCacheKey(
                        name,
                        location
                );

        CacheDto<List<Department>> cacheDto =
                new CacheDto<>();

        cacheDto.setData(
                (List<Department>) data
        );

        cacheDto.setAddedAt(
                LocalDateTime.now()
        );

        memory.put(key, cacheDto);
    }


    private boolean checkTTL(
            LocalDateTime accessedAt,
            Long ttl) {

        long minutes = Duration.between(
                accessedAt,
                LocalDateTime.now()
        ).toMinutes();

        return minutes < ttl;
    }


    @Override
    public void cleanUp() {

        log.info("Cleaning AllDepartmentsCache");

        LocalDateTime currentTime =
                LocalDateTime.now();

        memory.entrySet().removeIf(entry -> {

            CacheDto<List<Department>> cache =
                    entry.getValue();

            long minutes = Duration.between(
                    cache.getAddedAt(),
                    currentTime
            ).toMinutes();

            return minutes >= CACHE_TTL;
        });
    }


    @Override
    public void refreshData() {

        LocalDateTime currentTime =
                LocalDateTime.now();

        long minutes = Duration.between(
                lastUpdatedAt,
                currentTime
        ).toMinutes();

        if (minutes <= CACHE_REFRESH_TIME_MINUTES) {
            return;
        }

        for (Map.Entry<
                DepartmentCacheKey,
                CacheDto<List<Department>>> entry
                : memory.entrySet()) {

            DepartmentCacheKey key =
                    entry.getKey();

            CacheDto<List<Department>> cache =
                    entry.getValue();

            long idleMinutes = Duration.between(
                    cache.getAddedAt(),
                    currentTime
            ).toMinutes();

            // Don't refresh inactive cache
            if (idleMinutes >= CACHE_EVICT_TIME_MINUTES) {
                continue;
            }

            List<Department> departments =
                    departmentRepository
                            .findByNameAndLocation(
                                    key.name(),
                                    key.location()
                            );

            /*
             * Only update data.
             *
             * Do NOT update addedAt because
             * addedAt represents last access.
             */
            cache.setData(departments);
        }

        lastUpdatedAt = LocalDateTime.now();
    }
}