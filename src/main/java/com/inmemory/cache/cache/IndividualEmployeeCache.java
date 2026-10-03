package com.inmemory.cache.cache;

import com.inmemory.cache.dto.CacheDto;
import com.inmemory.cache.model.Employee;
import com.inmemory.cache.repositories.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("IndividualEmployeeCache")
@Slf4j
@RequiredArgsConstructor
public class IndividualEmployeeCache
        implements CacheMemory, CacheHandler,FastRefreshCache {

    private final EmployeeRepository employeeRepository;

    private final Map<Long, CacheDto<Employee>> memory =
            new ConcurrentHashMap<>();

    private LocalDateTime lastUpdatedAt =
            LocalDateTime.now();

    private final Long CACHE_TTL = 1L;
    private final Long CACHE_REFRESH_TIME_MINUTES = 5L;
    private final Long CACHE_EVICT_TIME_MINUTES = 10L;

    /*
     * AOP calls this method on cache lookup
     */
    @Override
    public Object get(Object[] args) {

        Long employeeId = (Long) args[0];

        CacheDto<Employee> cache =
                memory.get(employeeId);

        if (cache == null) {
            return null;
        }

        if (!checkTTL(
                cache.getAddedAt(),
                CACHE_TTL)) {

            memory.remove(employeeId);

            return null;
        }

        // Update last-access time
        cache.setAddedAt(LocalDateTime.now());

        return cache.getData();
    }


    /*
     * AOP calls this after DB method executes
     */
    @Override
    public void put(
            Object[] args,
            Object data) {

        Long employeeId = (Long) args[0];

        CacheDto<Employee> cacheDto =
                new CacheDto<>();

        cacheDto.setData(
                (Employee) data
        );

        cacheDto.setAddedAt(
                LocalDateTime.now()
        );

        memory.put(employeeId, cacheDto);
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

        log.info("Cleaning IndividualEmployeeCache");

        LocalDateTime currentTime =
                LocalDateTime.now();

        memory.entrySet().removeIf(entry -> {

            CacheDto<Employee> cache =
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

        for (Map.Entry<Long, CacheDto<Employee>> entry
                : memory.entrySet()) {

            Long employeeId =
                    entry.getKey();

            CacheDto<Employee> cache =
                    entry.getValue();

            long idleMinutes = Duration.between(
                    cache.getAddedAt(),
                    currentTime
            ).toMinutes();

            // Don't refresh inactive entry
            if (idleMinutes >= CACHE_EVICT_TIME_MINUTES) {
                continue;
            }

            Employee employee =
                    employeeRepository
                            .findById(employeeId)
                            .orElse(null);

            if (employee != null) {
                cache.setData(employee);
            }
        }

        lastUpdatedAt = LocalDateTime.now();
    }
}