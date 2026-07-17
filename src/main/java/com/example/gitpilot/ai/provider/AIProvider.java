package com.example.gitpilot.ai.provider;

public interface AIProvider {
    String generateInsight(String prompt);
    String getProviderName();
    String getModelName();
    boolean isAvailable();
}
