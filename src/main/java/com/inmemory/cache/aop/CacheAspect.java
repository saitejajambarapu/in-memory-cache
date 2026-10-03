package com.inmemory.cache.aop;
import com.inmemory.cache.annotation.MyCache;
import com.inmemory.cache.cache.CacheHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Map;
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheAspect {

    private final Map<String, CacheHandler> cacheMap;

    @Around("@annotation(myCache)")
    public Object handleCache(
            ProceedingJoinPoint joinPoint,
            MyCache myCache) throws Throwable {

        CacheHandler cache =
                cacheMap.get(myCache.value());

        if (cache == null) {
            throw new IllegalArgumentException(
                    "Cache not found: " + myCache.value()
            );
        }

        Object[] args = joinPoint.getArgs();

        // Cache failure should not break the application
        try {
            Object cachedData = cache.get(args);

            if (cachedData != null) {
                return cachedData;
            }

        } catch (Exception e) {

            log.error(
                    "Cache get failed for {}",
                    myCache.value(),
                    e
            );
        }

        // Actual service / DB call
        Object result = joinPoint.proceed();

        // Cache failure should not break the response
        try {
            cache.put(args, result);

        } catch (Exception e) {

            log.error(
                    "Cache put failed for {}",
                    myCache.value(),
                    e
            );
        }

        return result;
    }
}