package com.inmemory.cache.cache;

public interface CacheHandler {

    Object get(Object[] args);

    void put(Object[] args, Object data);
}