package com.example.gitpilot.ai.gateway;

import com.example.gitpilot.ai.dto.ProviderStats;
import com.example.gitpilot.ai.provider.AIProvider;
import com.example.gitpilot.ai.provider.GeminiProvider;
import com.example.gitpilot.ai.provider.GroqProvider;
import com.example.gitpilot.ai.provider.OpenRouterProvider;
import com.example.gitpilot.exception.AIUnavailableException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AIGatewayService {

    private final GeminiProvider geminiProvider;
    private final GroqProvider groqProvider;
    private final OpenRouterProvider openRouterProvider;

    private final Map<String, ProviderStats> statsMap = new ConcurrentHashMap<>();

    public AIGatewayService(GeminiProvider geminiProvider,
                            GroqProvider groqProvider,
                            OpenRouterProvider openRouterProvider) {
        this.geminiProvider = geminiProvider;
        this.groqProvider = groqProvider;
        this.openRouterProvider = openRouterProvider;

        statsMap.put(geminiProvider.getProviderName(), new ProviderStats(geminiProvider.getProviderName()));
        statsMap.put(groqProvider.getProviderName(), new ProviderStats(groqProvider.getProviderName()));
        statsMap.put(openRouterProvider.getProviderName(), new ProviderStats(openRouterProvider.getProviderName()));
    }

    public static class AIResult {
        public final String text;
        public final String providerUsed;
        public final String modelUsed;
        public final String fallbackUsed;

        public AIResult(String text, String providerUsed, String modelUsed, String fallbackUsed) {
            this.text = text;
            this.providerUsed = providerUsed;
            this.modelUsed = modelUsed;
            this.fallbackUsed = fallbackUsed;
        }
    }

    public AIResult generateInsightWithFailover(String prompt) {
        List<AIProvider> providers = List.of(geminiProvider, groqProvider, openRouterProvider);
        String fallbackUsed = "None";

        for (int i = 0; i < providers.size(); i++) {
            AIProvider provider = providers.get(i);
            if (!provider.isAvailable()) {
                continue;
            }

            long start = System.currentTimeMillis();
            ProviderStats stats = statsMap.get(provider.getProviderName());
            stats.getRequestCount().incrementAndGet();

            try {
                String result = provider.generateInsight(prompt);
                stats.getSuccessCount().incrementAndGet();
                stats.getTotalResponseTimeMs().addAndGet(System.currentTimeMillis() - start);

                String providerUsed = provider.getProviderName();
                if (i > 0) {
                    fallbackUsed = providerUsed;
                }
                return new AIResult(result, providerUsed, provider.getModelName(), fallbackUsed);
            } catch (Exception e) {
                stats.getFailureCount().incrementAndGet();
                stats.getTotalResponseTimeMs().addAndGet(System.currentTimeMillis() - start);

                if (e instanceof RestClientResponseException) {
                    RestClientResponseException rce = (RestClientResponseException) e;
                    if (rce.getStatusCode().value() == 429) {
                        stats.getRateLimitCount().incrementAndGet();
                    }
                }
            }
        }

        throw new AIUnavailableException("All AI providers failed to generate insight");
    }

    public Map<String, ProviderStats> getStatsMap() {
        return statsMap;
    }
}
