package com.example.gitpilot.webhook.service;

import com.example.gitpilot.github.client.GithubClient;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.webhook.entity.RepositoryWebhook;
import com.example.gitpilot.webhook.repository.RepositoryWebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookRegistrationService {

    private final GithubClient githubClient;
    private final RepositoryWebhookRepository webhookRepository;

    @Value("${github.webhook-secret:}")
    private String webhookSecret;

    @Value("${app.webhook-base-url:}")
    private String webhookBaseUrl;

    /**
     * Ensures a webhook exists for the given repository.
     * Creates one if missing, updates payload URL if changed.
     */
    @Transactional
    public void ensureWebhookForRepository(Repository repository, String accessToken) {
        String repoName = repository.getName();
        if (repoName == null || !repoName.contains("/")) {
            log.warn("[Webhook] Cannot register webhook for repository without owner/name format: {}", repoName);
            return;
        }

        String[] parts = repoName.split("/", 2);
        String owner = parts[0];
        String repo = parts[1];
        String payloadUrl = resolvePayloadUrl();

        if (payloadUrl == null || payloadUrl.isBlank()) {
            log.warn("[Webhook] No webhook base URL configured. Skipping webhook registration for {}", repoName);
            return;
        }

        log.info("[Webhook] Ensuring webhook for repository: {} payloadUrl={}", repoName, payloadUrl);

        try {
            // 1. Check if webhook already exists locally
            Optional<RepositoryWebhook> existingLocal = webhookRepository.findByRepository(repository);

            // 2. List hooks from GitHub to check for existing GitPilot hook
            List<Map<String, Object>> hooks = githubClient.listWebhooks(owner, repo, accessToken);
            Map<String, Object> existingHook = findGitPilotHook(hooks, payloadUrl);

            if (existingHook != null) {
                // Webhook exists on GitHub
                Long hookId = ((Number) existingHook.get("id")).longValue();
                Boolean active = (Boolean) existingHook.get("active");
                Map<?, ?> config = (Map<?, ?>) existingHook.get("config");
                String existingUrl = config != null ? (String) config.get("url") : "";

                if (!payloadUrl.equals(existingUrl)) {
                    // Update payload URL
                    log.info("[Webhook] Updating webhook payload URL for {} (hookId={})", repoName, hookId);
                    githubClient.updateWebhook(owner, repo, accessToken, hookId, payloadUrl, webhookSecret);
                } else {
                    log.info("[Webhook] Webhook already exists for {} (hookId={})", repoName, hookId);
                }

                // Persist or update local record
                RepositoryWebhook webhook = existingLocal.orElse(new RepositoryWebhook());
                webhook.setRepository(repository);
                webhook.setGithubWebhookId(hookId);
                webhook.setPayloadUrl(payloadUrl);
                webhook.setActive(active != null ? active : true);
                webhook.setEvents("push");
                webhook.setUpdatedAt(LocalDateTime.now());
                if (webhook.getCreatedAt() == null) webhook.setCreatedAt(LocalDateTime.now());
                webhookRepository.save(webhook);

            } else {
                // Create new webhook
                log.info("[Webhook] Creating new webhook for {}", repoName);
                Map<String, Object> created = githubClient.createWebhook(owner, repo, accessToken, payloadUrl, webhookSecret);

                if (created != null && created.containsKey("id")) {
                    Long hookId = ((Number) created.get("id")).longValue();
                    Boolean active = (Boolean) created.get("active");

                    RepositoryWebhook webhook = existingLocal.orElse(new RepositoryWebhook());
                    webhook.setRepository(repository);
                    webhook.setGithubWebhookId(hookId);
                    webhook.setPayloadUrl(payloadUrl);
                    webhook.setActive(active != null ? active : true);
                    webhook.setEvents("push");
                    webhook.setCreatedAt(LocalDateTime.now());
                    webhook.setUpdatedAt(LocalDateTime.now());
                    webhookRepository.save(webhook);

                    log.info("[Webhook] Webhook created successfully for {} (hookId={})", repoName, hookId);
                } else {
                    log.error("[Webhook] Failed to create webhook for {} - no id returned", repoName);
                }
            }
        } catch (Exception e) {
            log.error("[Webhook] Failed to register webhook for {}: {}", repoName, e.getMessage());
        }
    }

    /**
     * Find a hook that matches our payload URL pattern.
     */
    private Map<String, Object> findGitPilotHook(List<Map<String, Object>> hooks, String payloadUrl) {
        for (Map<String, Object> hook : hooks) {
            Map<?, ?> config = (Map<?, ?>) hook.get("config");
            if (config != null) {
                String url = (String) config.get("url");
                if (url != null && (url.equals(payloadUrl) || url.contains("/webhooks/github"))) {
                    return hook;
                }
            }
        }
        return null;
    }

    private String resolvePayloadUrl() {
        if (webhookBaseUrl != null && !webhookBaseUrl.isBlank()) {
            String base = webhookBaseUrl.endsWith("/") ? webhookBaseUrl.substring(0, webhookBaseUrl.length() - 1) : webhookBaseUrl;
            return base + "/webhooks/github";
        }
        return null;
    }
}
