package com.example.gitpilot.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
public class ProviderStats {
    private String providerName;
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong rateLimitCount = new AtomicLong(0);
    private final AtomicLong totalResponseTimeMs = new AtomicLong(0);

    public ProviderStats(String providerName) {
        this.providerName = providerName;
    }

    public double getAverageResponseTimeMs() {
        long count = requestCount.get();
        return count == 0 ? 0.0 : (double) totalResponseTimeMs.get() / count;
    }
}
