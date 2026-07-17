package com.example.gitpilot.ai.cache;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AICacheService {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long ttlMinutes = 60; // 1 hour validity

    private String getCacheKey(Long repositoryId, String reportType) {
        return repositoryId + "_" + reportType;
    }

    public CacheEntry get(Long repositoryId, String reportType) {
        CacheEntry entry = cache.get(getCacheKey(repositoryId, reportType));
        if (entry != null && entry.isValid(ttlMinutes)) {
            return entry;
        }
        return null;
    }

    public void put(Long repositoryId, String reportType, String content, String providerUsed, String model, String fallbackUsed) {
        cache.put(getCacheKey(repositoryId, reportType), new CacheEntry(content, providerUsed, model, fallbackUsed, LocalDateTime.now()));
    }
}
