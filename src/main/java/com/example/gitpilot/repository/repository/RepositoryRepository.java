package com.example.gitpilot.repository.repository;

import com.example.gitpilot.dashboard.dto.RepositoryAnalyticsResponse;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RepositoryRepository extends JpaRepository<Repository, Long> {
    Optional<Repository> findByGithubRepoId(Long githubRepoId);
    List<Repository> findByUser(User user);
    List<Repository> findByUserAndSelectedTrue(User user);
    Long countByUserAndSelectedTrue(User user);

    @Query("SELECT MAX(r.updatedAt) FROM Repository r WHERE r.user = :user AND r.selected = true")
    LocalDateTime findLastSynchronizationByUser(@Param("user") User user);

    @Query("SELECT new com.example.gitpilot.dashboard.dto.RepositoryAnalyticsResponse(" +
           "r.id, r.name, COUNT(c), MAX(c.commitDate), COUNT(DISTINCT c.authorEmail)) " +
           "FROM Repository r LEFT JOIN Commit c ON c.repository = r " +
           "WHERE r.user = :user AND r.selected = true " +
           "GROUP BY r.id, r.name")
    List<RepositoryAnalyticsResponse> findRepositoryAnalyticsByUser(@Param("user") User user);
}
