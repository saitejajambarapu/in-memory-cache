package com.inmemory.cache.cache;

import com.inmemory.cache.dto.CacheDto;
import com.inmemory.cache.model.Employee;
import com.inmemory.cache.repositories.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("AllEmployeesCache")
@Slf4j
@RequiredArgsConstructor
public class AllEmployeesCache
        implements CacheMemory, CacheHandler,SlowRefreshCache {

    private final EmployeeRepository employeeRepository;

    private final Map<String, CacheDto<List<Employee>>> memory =
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

        String name = "ALL";

        CacheDto<List<Employee>> cache =
                memory.get(name);

        if (cache == null) {
            return null;
        }

        if (!checkTTL(
                cache.getAddedAt(),
                CACHE_TTL)) {

            memory.remove(name);

            return null;
        }

        // addedAt = last accessed time
        cache.setAddedAt(LocalDateTime.now());

        return cache.getData();
    }


    /*
     * AOP calls this after DB method executes
     */
    @Override
    @SuppressWarnings("unchecked")
    public void put(
            Object[] args,
            Object data) {

        String name ="ALL";

        CacheDto<List<Employee>> cacheDto =
                new CacheDto<>();

        cacheDto.setData(
                (List<Employee>) data
        );

        cacheDto.setAddedAt(
                LocalDateTime.now()
        );

        memory.put(name, cacheDto);
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

        log.info("Cleaning AllEmployeesCache");

        LocalDateTime currentTime =
                LocalDateTime.now();

        memory.entrySet().removeIf(entry -> {

            CacheDto<List<Employee>> cache =
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

        CacheDto<List<Employee>> cache =
                memory.get("All");

        if (cache == null) {
            lastUpdatedAt = LocalDateTime.now();
            return;
        }

        long idleMinutes = Duration.between(
                cache.getAddedAt(),
                currentTime
        ).toMinutes();

        // Don't refresh inactive cache
        if (idleMinutes >= CACHE_EVICT_TIME_MINUTES) {
            return;
        }

        List<Employee> employees =
                employeeRepository.findAll();

        cache.setData(employees);

        // Don't update addedAt here.
        // It represents last access.

        lastUpdatedAt = LocalDateTime.now();
    }
}