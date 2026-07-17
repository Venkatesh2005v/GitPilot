package com.example.gitpilot.dashboard.controller;

import com.example.gitpilot.commit.dto.CommitResponse;
import com.example.gitpilot.dashboard.dto.*;
import com.example.gitpilot.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Dashboard & Analytics", description = "Endpoints for developer dashboard summary and repository analytics")
@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Get repositories analytics", description = "Returns aggregates for all selected repositories of the authenticated user")
    @GetMapping("/dashboard/repositories")
    public ResponseEntity<List<RepositoryAnalyticsResponse>> getRepositoriesAnalytics(
            @AuthenticationPrincipal OAuth2User user
    ) {
        List<RepositoryAnalyticsResponse> analytics = dashboardService.getRepositoriesAnalytics(user);
        return ResponseEntity.ok(analytics);
    }

    @Operation(summary = "Get repository activity", description = "Returns activity metrics for a specific repository")
    @GetMapping("/repositories/{repositoryId}/activity")
    public ResponseEntity<RepositoryActivityResponse> getRepositoryActivity(
            @PathVariable Long repositoryId
    ) {
        RepositoryActivityResponse activity = dashboardService.getRepositoryActivity(repositoryId);
        return ResponseEntity.ok(activity);
    }

    @Operation(summary = "Get repository commits", description = "Returns paginated list of commits for a specific repository ordered by date descending")
    @GetMapping("/repositories/{repositoryId}/commits")
    public ResponseEntity<List<CommitResponse>> getRepositoryCommits(
            @PathVariable Long repositoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<CommitResponse> commits = dashboardService.getRepositoryCommits(repositoryId, page, size);
        return ResponseEntity.ok(commits);
    }

    @Operation(summary = "Get dashboard summary", description = "Returns overall stats summary for the user's dashboard")
    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @AuthenticationPrincipal OAuth2User user
    ) {
        DashboardSummaryResponse summary = dashboardService.getDashboardSummary(user);
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Get repository contributors", description = "Returns list of contributors and their commit counts for a specific repository")
    @GetMapping("/repositories/{repositoryId}/contributors")
    public ResponseEntity<List<ContributorResponse>> getRepositoryContributors(
            @PathVariable Long repositoryId
    ) {
        List<ContributorResponse> contributors = dashboardService.getRepositoryContributors(repositoryId);
        return ResponseEntity.ok(contributors);
    }
}
