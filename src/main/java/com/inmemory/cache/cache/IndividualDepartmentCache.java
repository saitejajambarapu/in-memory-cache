package com.inmemory.cache.cache;

import com.inmemory.cache.dto.CacheDto;
import com.inmemory.cache.model.Department;
import com.inmemory.cache.repositories.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("IndividualDepartmentCache")
@Slf4j
@RequiredArgsConstructor
public class IndividualDepartmentCache
        implements CacheMemory, CacheHandler,FastRefreshCache {

    private final DepartmentRepository departmentRepository;

    /*
     * departmentId -> department
     */
    private final Map<
            Long,
            CacheDto<Department>
            > memory = new ConcurrentHashMap<>();

    private LocalDateTime lastUpdatedAt =
            LocalDateTime.now();

    private final Long CACHE_TTL = 3L;

    private final Long CACHE_REFRESH_TIME_MINUTES = 5L;

    private final Long CACHE_EVICT_TIME_MINUTES = 10L;




    /*
     * Called by AOP during cache lookup
     */
    @Override
    public Object get(Object[] args) {

        Long departmentId =
                (Long) args[0];

        CacheDto<Department> cache =
                memory.get(departmentId);

        if (cache == null) {
            return null;
        }

        if (!checkTTL(
                cache.getAddedAt(),
                CACHE_TTL)) {

            memory.remove(departmentId);

            return null;
        }

        // Update last-accessed time
        cache.setAddedAt(LocalDateTime.now());

        return cache.getData();
    }


    /*
     * Called by AOP after DB call
     */
    @Override
    public void put(
            Object[] args,
            Object data) {

        Long departmentId =
                (Long) args[0];

        CacheDto<Department> cacheDto =
                new CacheDto<>();

        cacheDto.setData(
                (Department) data
        );

        cacheDto.setAddedAt(
                LocalDateTime.now()
        );

        memory.put(
                departmentId,
                cacheDto
        );
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

        log.info("Cleaning IndividualDepartmentCache");

        LocalDateTime currentTime =
                LocalDateTime.now();

        memory.entrySet().removeIf(entry -> {

            CacheDto<Department> cache =
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
                Long,
                CacheDto<Department>> entry
                : memory.entrySet()) {

            Long departmentId =
                    entry.getKey();

            CacheDto<Department> cache =
                    entry.getValue();

            long idleMinutes = Duration.between(
                    cache.getAddedAt(),
                    currentTime
            ).toMinutes();

            // Don't refresh inactive entry
            if (idleMinutes >= CACHE_EVICT_TIME_MINUTES) {
                continue;
            }

            Department department =
                    departmentRepository
                            .findById(departmentId)
                            .orElse(null);

            if (department != null) {
                /*
                 * Only update data.
                 *
                 * Do NOT update addedAt.
                 */
                cache.setData(department);
            }
        }

        lastUpdatedAt = LocalDateTime.now();
    }
}