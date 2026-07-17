package com.example.gitpilot.dashboard.service;

import com.example.gitpilot.commit.dto.CommitResponse;
import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.dashboard.dto.*;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import com.example.gitpilot.user.entity.User;
import com.example.gitpilot.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final RepositoryRepository repositoryRepository;
    private final CommitRepository commitRepository;

    public DashboardService(UserRepository userRepository,
                            RepositoryRepository repositoryRepository,
                            CommitRepository commitRepository) {
        this.userRepository = userRepository;
        this.repositoryRepository = repositoryRepository;
        this.commitRepository = commitRepository;
    }

    private User getAuthenticatedUser(OAuth2User oauthUser) {
        Map<String, Object> attributes = oauthUser.getAttributes();
        Long githubId = ((Number) attributes.get("id")).longValue();
        return userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with githubId: " + githubId));
    }

    public List<RepositoryAnalyticsResponse> getRepositoriesAnalytics(OAuth2User oauthUser) {
        User user = getAuthenticatedUser(oauthUser);
        return repositoryRepository.findRepositoryAnalyticsByUser(user);
    }

    public RepositoryActivityResponse getRepositoryActivity(Long repositoryId) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found with id: " + repositoryId));

        Long totalCommits = commitRepository.countByRepository(repository);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Long commitsLast7Days = commitRepository.countByRepositoryAndCommitDateAfter(repository, sevenDaysAgo);

        Commit latestCommit = commitRepository.findFirstByRepositoryOrderByCommitDateDesc(repository).orElse(null);
        String latestCommitMessage = latestCommit != null ? latestCommit.getMessage() : null;
        String latestCommitAuthor = latestCommit != null ? latestCommit.getAuthorName() : null;
        LocalDateTime latestCommitDate = latestCommit != null ? latestCommit.getCommitDate() : null;

        return new RepositoryActivityResponse(
                repository.getName(),
                totalCommits,
                commitsLast7Days,
                latestCommitMessage,
                latestCommitAuthor,
                latestCommitDate
        );
    }

    public List<CommitResponse> getRepositoryCommits(Long repositoryId, int page, int size) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found with id: " + repositoryId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Commit> commitPage = commitRepository.findByRepositoryOrderByCommitDateDesc(repository, pageable);

        return commitPage.getContent().stream()
                .map(commit -> new CommitResponse(
                        commit.getGithubCommitSha(),
                        commit.getMessage(),
                        commit.getAuthorName(),
                        commit.getAuthorEmail(),
                        commit.getCommitDate(),
                        commit.getCommitUrl()
                ))
                .toList();
    }

    public DashboardSummaryResponse getDashboardSummary(OAuth2User oauthUser) {
        User user = getAuthenticatedUser(oauthUser);

        Long selectedRepositories = repositoryRepository.countByUserAndSelectedTrue(user);
        Long totalCommits = commitRepository.countCommitsByUser(user);

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Long commitsLast7Days = commitRepository.countCommitsByUserSince(user, sevenDaysAgo);

        List<String> activeRepoNames = commitRepository.findMostActiveRepositoryName(user, PageRequest.of(0, 1));
        String mostActiveRepository = activeRepoNames.isEmpty() ? "-" : activeRepoNames.get(0);

        LocalDateTime lastSynchronization = repositoryRepository.findLastSynchronizationByUser(user);

        return new DashboardSummaryResponse(
                selectedRepositories,
                totalCommits,
                commitsLast7Days,
                mostActiveRepository,
                lastSynchronization
        );
    }

    public List<ContributorResponse> getRepositoryContributors(Long repositoryId) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found with id: " + repositoryId));

        return commitRepository.findContributorsByRepository(repository);
    }
}
