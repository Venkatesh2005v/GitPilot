package com.example.gitpilot.github.client;

import com.example.gitpilot.github.dto.GithubCommitResponse;
import com.example.gitpilot.github.dto.GithubRepositoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
public class GithubClient {

    private final RestClient restClient;

    public GithubClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<GithubRepositoryResponse> getRepositories(String accessToken) {
        log.info("[GitHubAPI] GET /user/repos");
        long start = System.currentTimeMillis();
        var result = restClient
                .get()
                .uri("https://api.github.com/user/repos")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "GitPilot-Application")
                .retrieve()
                .body(new ParameterizedTypeReference<List<GithubRepositoryResponse>>() {});
        log.info("[GitHubAPI] GET /user/repos completed in {}ms, returned {} repos", System.currentTimeMillis() - start, result != null ? result.size() : 0);
        return result;
    }

    public List<GithubCommitResponse> getCommits(String owner, String repo, String accessToken) {
        log.info("[GitHubAPI] GET /repos/{}/{}/commits", owner, repo);
        var spec = restClient.get()
                .uri("https://api.github.com/repos/{owner}/{repo}/commits", owner, repo)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "GitPilot-Application");

        if (accessToken != null && !accessToken.isBlank()) {
            spec = spec.header("Authorization", "Bearer " + accessToken);
        }

        return spec.retrieve()
                .body(new ParameterizedTypeReference<List<GithubCommitResponse>>() {});
    }

    public List<GithubCommitResponse> getPublicCommits(String owner, String repo) {
        return getCommits(owner, repo, null);
    }

    public java.util.Map<String, Long> getLanguages(String owner, String repo, String accessToken) {
        log.info("[GitHubAPI] GET /repos/{}/{}/languages", owner, repo);
        try {
            var spec = restClient.get()
                    .uri("https://api.github.com/repos/{owner}/{repo}/languages", owner, repo)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "GitPilot-Application");

            if (accessToken != null && !accessToken.isBlank()) {
                spec = spec.header("Authorization", "Bearer " + accessToken);
            }

            var result = spec.retrieve()
                    .body(new ParameterizedTypeReference<java.util.Map<String, Long>>() {});
            log.info("[GitHubAPI] Languages for {}/{}: {}", owner, repo, result);
            return result != null ? result : java.util.Map.of();
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("[GitHubAPI] Languages failed for {}/{}: HTTP {} body={}", owner, repo, e.getStatusCode().value(), e.getResponseBodyAsString());
            return java.util.Map.of();
        } catch (Exception e) {
            log.error("[GitHubAPI] Languages failed for {}/{}: {}", owner, repo, e.getMessage());
            return java.util.Map.of();
        }
    }

    public String getReadme(String owner, String repo, String accessToken) {
        log.info("[GitHubAPI] GET /repos/{}/{}/readme", owner, repo);
        try {
            var spec = restClient.get()
                    .uri("https://api.github.com/repos/{owner}/{repo}/readme", owner, repo)
                    .header("Accept", "application/vnd.github.raw+json")
                    .header("User-Agent", "GitPilot-Application");

            if (accessToken != null && !accessToken.isBlank()) {
                spec = spec.header("Authorization", "Bearer " + accessToken);
            }

            String result = spec.retrieve().body(String.class);
            log.info("[GitHubAPI] README for {}/{}: found={} length={}", owner, repo, result != null && !result.isEmpty(), result != null ? result.length() : 0);
            return result != null ? result : "";
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("[GitHubAPI] README failed for {}/{}: HTTP {} body={}", owner, repo, e.getStatusCode().value(), e.getResponseBodyAsString());
            return "";
        } catch (Exception e) {
            log.error("[GitHubAPI] README failed for {}/{}: {}", owner, repo, e.getMessage());
            return "";
        }
    }

    public List<String> getRepoContents(String owner, String repo, String accessToken) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/contents";
        log.info("[GitHubAPI] GET {}", url);
        try {
            var spec = restClient.get()
                    .uri("https://api.github.com/repos/{owner}/{repo}/contents", owner, repo)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "GitPilot-Application");

            if (accessToken != null && !accessToken.isBlank()) {
                spec = spec.header("Authorization", "Bearer " + accessToken);
            }

            var result = spec.retrieve()
                    .body(new ParameterizedTypeReference<List<java.util.Map<String, Object>>>() {});
            if (result == null) return List.of();
            List<String> names = result.stream()
                    .map(item -> (String) item.get("name"))
                    .filter(java.util.Objects::nonNull)
                    .toList();
            log.info("[GitHubAPI] Contents for {}/{}: {} files", owner, repo, names.size());
            return names;
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("[GitHubAPI] Contents failed for {}/{}: HTTP {} body={}", owner, repo, e.getStatusCode().value(), e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.error("[GitHubAPI] Contents failed for {}/{}: {}", owner, repo, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get contents of a specific path within a repository.
     */
    public List<String> getRepoContentsAtPath(String owner, String repo, String path, String accessToken) {
        try {
            var spec = restClient.get()
                    .uri("https://api.github.com/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "GitPilot-Application");

            if (accessToken != null && !accessToken.isBlank()) {
                spec = spec.header("Authorization", "Bearer " + accessToken);
            }

            var result = spec.retrieve()
                    .body(new ParameterizedTypeReference<List<java.util.Map<String, Object>>>() {});
            if (result == null) return List.of();
            return result.stream()
                    .map(item -> (String) item.get("name"))
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    // ======================== Webhook Management ========================

    /**
     * List all webhooks for a repository.
     */
    public List<java.util.Map<String, Object>> listWebhooks(String owner, String repo, String accessToken) {
        try {
            var result = restClient.get()
                    .uri("https://api.github.com/repos/{owner}/{repo}/hooks", owner, repo)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "GitPilot-Application")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<java.util.Map<String, Object>>>() {});
            return result != null ? result : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Create a new webhook for a repository.
     * Returns the created webhook as a Map, or null on failure.
     */
    public java.util.Map<String, Object> createWebhook(String owner, String repo, String accessToken,
                                                         String payloadUrl, String secret) {
        log.info("[GitHubAPI] POST /repos/{}/{}/hooks payloadUrl={}", owner, repo, payloadUrl);
        var body = java.util.Map.of(
                "name", "web",
                "active", true,
                "events", List.of("push"),
                "config", java.util.Map.of(
                        "url", payloadUrl,
                        "content_type", "application/json",
                        "secret", secret != null ? secret : "",
                        "insecure_ssl", "0"
                )
        );

        return restClient.post()
                .uri("https://api.github.com/repos/{owner}/{repo}/hooks", owner, repo)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "GitPilot-Application")
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<java.util.Map<String, Object>>() {});
    }

    /**
     * Update an existing webhook's config.
     */
    public java.util.Map<String, Object> updateWebhook(String owner, String repo, String accessToken,
                                                         Long hookId, String payloadUrl, String secret) {
        var body = java.util.Map.of(
                "active", true,
                "events", List.of("push"),
                "config", java.util.Map.of(
                        "url", payloadUrl,
                        "content_type", "application/json",
                        "secret", secret != null ? secret : "",
                        "insecure_ssl", "0"
                )
        );

        return restClient.patch()
                .uri("https://api.github.com/repos/{owner}/{repo}/hooks/{hookId}", owner, repo, hookId)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "GitPilot-Application")
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<java.util.Map<String, Object>>() {});
    }
}