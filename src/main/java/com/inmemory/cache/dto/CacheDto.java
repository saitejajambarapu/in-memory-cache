package com.inmemory.cache.dto;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class CacheDto<T> {

    private T data;

    private LocalDateTime addedAt;

    private LocalDateTime updatedAt;
}
