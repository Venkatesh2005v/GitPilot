package com.example.gitpilot.webhook.service;

import com.example.gitpilot.ai.cache.AICacheService;
import com.example.gitpilot.ai.repository.AIReportRepository;
import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final RepositoryRepository repositoryRepository;
    private final CommitRepository commitRepository;
    private final AICacheService aiCacheService;
    private final AIReportRepository aiReportRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${github.webhook-secret:}")
    private String secret;

    public WebhookService(RepositoryRepository repositoryRepository,
                          CommitRepository commitRepository,
                          AICacheService aiCacheService,
                          AIReportRepository aiReportRepository) {
        this.repositoryRepository = repositoryRepository;
        this.commitRepository = commitRepository;
        this.aiCacheService = aiCacheService;
        this.aiReportRepository = aiReportRepository;
    }


    public boolean verifySignature(String payload, String signatureHeader) {
        if (secret == null || secret.trim().isEmpty()) {
            log.warn("GitHub Webhook secret is not configured. Bypassing signature verification.");
            return true;
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            log.error("Invalid signature header format. Header must start with 'sha256='");
            return false;
        }
        String expectedSignature = signatureHeader.substring(7);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = HexFormat.of().formatHex(rawHmac);
            return MessageDigest.isEqual(computedSignature.getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error computing signature hash: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public void processPushWebhook(String payload) throws Exception {
        Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
        Map<String, Object> repoMap = (Map<String, Object>) payloadMap.get("repository");
        if (repoMap == null) {
            throw new IllegalArgumentException("Invalid payload: repository object is missing");
        }

        Number repoIdNum = (Number) repoMap.get("id");
        if (repoIdNum == null) {
            throw new IllegalArgumentException("Invalid payload: repository.id is missing");
        }
        Long githubRepoId = repoIdNum.longValue();

        Optional<Repository> repoOpt = repositoryRepository.findByGithubRepoId(githubRepoId);
        if (repoOpt.isEmpty()) {
            log.info("Repository with githubRepoId {} is not registered. Ignoring webhook.", githubRepoId);
            return;
        }

        Repository repository = repoOpt.get();
        if (!Boolean.TRUE.equals(repository.getSelected())) {
            log.info("Repository {} is not selected for tracking. Ignoring webhook.", repository.getName());
            return;
        }

        long startTime = System.currentTimeMillis();
        repository.setLastSyncStatus("RUNNING");
        repositoryRepository.saveAndFlush(repository);

        try {
            // 1. Update Repository Metadata
            String name = (String) repoMap.get("name");
            String htmlUrl = (String) repoMap.get("html_url");
            String defaultBranch = (String) repoMap.get("default_branch");
            Boolean privateRepo = (Boolean) repoMap.get("private");

            if (name != null) repository.setName(name);
            if (htmlUrl != null) repository.setHtmlUrl(htmlUrl);
            if (defaultBranch != null) repository.setDefaultBranch(defaultBranch);
            if (privateRepo != null) repository.setPrivateRepo(privateRepo);

            // 2. Update Commits
            List<Map<String, Object>> commitsList = (List<Map<String, Object>>) payloadMap.get("commits");
            int addedCount = 0;
            if (commitsList != null) {
                for (Map<String, Object> commitMap : commitsList) {
                    String sha = (String) commitMap.get("id");
                    if (sha == null) continue;

                    // Prevent duplicate commits
                    if (commitRepository.findByGithubCommitSha(sha).isPresent()) {
                        continue;
                    }

                    Commit commit = new Commit();
                    commit.setGithubCommitSha(sha);
                    commit.setCommitUrl((String) commitMap.get("url"));
                    commit.setMessage((String) commitMap.get("message"));

                    Map<String, Object> authorMap = (Map<String, Object>) commitMap.get("author");
                    if (authorMap != null) {
                        commit.setAuthorName((String) authorMap.get("name"));
                        commit.setAuthorEmail((String) authorMap.get("email"));
                        String timestampStr = (String) authorMap.get("timestamp");
                        if (timestampStr != null) {
                            try {
                                commit.setCommitDate(OffsetDateTime.parse(timestampStr).toLocalDateTime());
                            } catch (Exception e) {
                                commit.setCommitDate(LocalDateTime.now());
                            }
                        } else {
                            commit.setCommitDate(LocalDateTime.now());
                        }
                    } else {
                        commit.setCommitDate(LocalDateTime.now());
                    }

                    commit.setRepository(repository);
                    commitRepository.save(commit);
                    addedCount++;
                }
            }
            log.info("Webhook processed: added {} commits for repository {}", addedCount, repository.getName());

            // 3. Invalidate AI Cache
            aiCacheService.evict(repository.getId());
            aiReportRepository.deleteByRepositoryAndReportType(repository, "FULL_REPORT");

            // 4. Update Synchronization Metadata
            long duration = System.currentTimeMillis() - startTime;
            repository.setLastSyncedAt(LocalDateTime.now());
            repository.setLastSyncStatus("SUCCESS");
            repository.setLastSyncDuration(duration);
            repository.setLastSyncError(null);
            repositoryRepository.save(repository);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            repository.setLastSyncedAt(LocalDateTime.now());
            repository.setLastSyncStatus("FAILED");
            repository.setLastSyncDuration(duration);
            repository.setLastSyncError(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            repositoryRepository.save(repository);
            throw e;
        }
    }
}
