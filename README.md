# Spring Boot Custom In-Memory Cache

A custom in-memory caching solution built with Spring Boot using
ConcurrentHashMap, custom AOP, and scheduled cache maintenance.

## Overview

This project implements an application-level in-memory cache without using
external caching systems such as Redis.

The cache is designed to:

- Reduce repeated database calls
- Provide fast access to frequently requested data
- Automatically populate the cache on cache miss
- Refresh active cache entries from the database
- Remove inactive/expired cache entries
- Keep cache logic separate from business logic
- Support different cache implementations for different data types

## Architecture

The main architecture is:

Controller
↓
Service
↓
@MyCache
↓
CacheAspect
↓
Map<String, CacheHandler>
↓
Specific Cache
↓
ConcurrentHashMap

Background maintenance:

Scheduler
↓
CacheMemory
↓
cleanUp() / refreshData()
↓
Specific Cache

## Cache Design

Each cache has its own cache class and its own ConcurrentHashMap.

Example:

Employee
├── AllEmployeesCache
│   └── Map<String, CacheDto<List<Employee>>>
│
└── IndividualEmployeeCache
└── Map<Long, CacheDto<Employee>>


Department
├── AllDepartmentsCache
│   └── Map<DepartmentCacheKey, CacheDto<List<Department>>>
│
└── IndividualDepartmentCache
└── Map<Long, CacheDto<Department>>

This keeps the cache implementation independent and allows each cache to
define its own key structure, TTL, refresh interval, cleanup policy, and
database query.

## AOP-Based Cache

A custom annotation is used to enable caching on service methods.

Example:

@MyCache("IndividualEmployeeCache")
public Employee getEmployeeById(Long id) {
return employeeRepository.findById(id)
.orElseThrow();
}

The CacheAspect intercepts the method and performs the cache-aside logic.

### Cache Hit

Request
↓
CacheAspect
↓
Cache lookup
↓
Data found
↓
Return cached data

The actual service method is not executed.

### Cache Miss

Request
↓
CacheAspect
↓
Cache lookup
↓
Data not found
↓
Execute service method
↓
Database
↓
Store result in cache
↓
Return result

The service layer therefore contains only business/database logic and does
not need to contain cache lookup or cache population code.

## CacheHandler

CacheHandler is used by the AOP layer for request-time cache operations.

Typical responsibilities:

- get()
- put()

Each cache implementation handles its own arguments and cache key creation.

For example, a parameterized department cache can create:

DepartmentCacheKey(name, location)

while an individual employee cache can directly use:

Long employeeId

## CacheMemory

CacheMemory is used for background cache maintenance.

It contains operations such as:

- cleanUp()
- refreshData()

This separates request-time caching from background cache maintenance.

## Cache Timing

Each cache maintains its own timing configuration.

Two important timestamps are maintained conceptually:

### lastAccessedAt

Tracks when the cache entry was last used by a request.

This is used to determine whether an entry is inactive.

### lastUpdatedAt

Tracks when the cache was last refreshed from the database.

Background refresh updates the cached data but does not update the
last-accessed time.

## Cache Cleanup

Cleanup removes inactive cache entries.

The cleanup scheduler runs every 5 minutes:

    Scheduler
        ↓
    CacheMemory
        ↓
    cleanUp()

Each cache determines its own expiration/inactivity policy.

If one cache fails during cleanup, the exception is handled independently so
that other caches can continue processing.

## Cache Refresh

Caches are divided into different refresh categories based on how frequently
their underlying data changes.

### Fast Refresh

Used for frequently changing or required data.

Example:

    Fast Refresh Scheduler
            ↓
       FastRefreshCache
            ↓
        refreshData()

### Slow Refresh

Used for relatively static data.

Example:

    Slow Refresh Scheduler
            ↓
       SlowRefreshCache
            ↓
        refreshData()

The scheduler only triggers the refresh operation. Each cache determines
whether it actually needs to refresh based on its own configuration.

## Scheduled Tasks

The current scheduling strategy is:

| Task | Frequency | Purpose |
|------|-----------|---------|
| Cleanup | Every 5 minutes | Remove inactive cache entries |
| Fast Refresh | Frequent | Refresh frequently changing data |
| Slow Refresh | Less frequent | Refresh static data |

The application can contain many cache maps without requiring a separate
scheduler for every cache.

## Thread Safety

ConcurrentHashMap is used for cache storage so that multiple application
threads can safely access and modify cache entries concurrently.

The cache is designed for concurrent application requests and background
maintenance.

## Error Handling

Cache failures should not normally prevent the application from retrieving
data from the database.

The AOP layer handles cache get/put failures separately.

If a cache lookup fails:

    Cache failure
        ↓
    Log error
        ↓
    Execute service method
        ↓
    Database result

If cache population fails:

    Database result
        ↓
    Cache put failure
        ↓
    Log error
        ↓
    Return database result

Database/service exceptions are allowed to propagate normally.

## Project Structure

Example structure:

src/main/java/com/inmemory/cache

├── annotation
│   └── MyCache.java
│
├── aspect
│   └── CacheAspect.java
│
├── cache
│   ├── AllEmployeesCache.java
│   ├── IndividualEmployeeCache.java
│   ├── AllDepartmentsCache.java
│   ├── IndividualDepartmentCache.java
│   ├── CacheHandler.java
│   └── CacheMemory.java
│
├── controller
│   ├── EmployeeController.java
│   └── DepartmentController.java
│
├── dto
│   └── CacheDto.java
│
├── model
│   ├── Employee.java
│   └── Department.java
│
├── record
│   └── DepartmentCacheKey.java
│
├── repositories
│   ├── EmployeeRepository.java
│   └── DepartmentRepository.java
│
└── service
├── EmployeeService.java
└── DepartmentService.java

## Key Design Decisions

### Individual Cache Classes

Each cache has its own class instead of keeping multiple unrelated maps in a
single cache class.

This makes the cache easier to identify, maintain, modify, and configure.

### AOP

AOP removes repetitive cache-aside code from service methods.

### Spring Bean Map Injection

Spring automatically injects cache implementations into:

Map<String, CacheHandler>

The key is the Spring bean name.

For example:

@Component("IndividualDepartmentCache")

can be accessed through:

cacheMap.get("IndividualDepartmentCache")

### Separate Refresh Groups

Fast-changing and static data do not need the same refresh frequency.

Separate refresh interfaces allow the scheduler to process them independently.

## Technologies

- Java
- Spring Boot
- Spring AOP
- Spring Data JPA
- ConcurrentHashMap
- Scheduled Tasks
- Lombok
- Relational Database

## Important Considerations

This is an application-local cache.

If multiple instances of the application are running, each instance has its
own cache memory.

Therefore, changes made in one application instance are not automatically
propagated to the cache of another instance.

The cache should therefore be used for data where application-local caching
is acceptable.

## Future Improvements

Possible improvements include:

- Cache metrics
- Cache hit/miss statistics
- Configurable refresh intervals
- Configurable cleanup policies
- Async/virtual-thread based refresh
- Cache warm-up during application startup
- Better handling of concurrent cache misses
- Distributed cache if cross-instance consistency becomes necessary