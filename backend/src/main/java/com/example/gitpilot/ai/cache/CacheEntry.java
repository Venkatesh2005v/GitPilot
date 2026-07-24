package com.example.gitpilot.ai.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CacheEntry {
    private final String content;
    private final String providerUsed;
    private final String model;
    private final String fallbackUsed;
    private final LocalDateTime generatedTime;

    public boolean isValid(long ttlMinutes) {
        return generatedTime.plusMinutes(ttlMinutes).isAfter(LocalDateTime.now());
    }
}
