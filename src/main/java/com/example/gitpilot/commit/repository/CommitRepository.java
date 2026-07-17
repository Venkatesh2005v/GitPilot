package com.example.gitpilot.commit.repository;

import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.dashboard.dto.ContributorResponse;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommitRepository extends JpaRepository<Commit, Long> {
    Optional<Commit> findByGithubCommitSha(String sha);
    List<Commit> findByRepository(Repository repository);
    Long countByRepository(Repository repository);
    Long countByRepositoryAndCommitDateAfter(Repository repository, LocalDateTime dateTime);
    Optional<Commit> findFirstByRepositoryOrderByCommitDateDesc(Repository repository);

    Page<Commit> findByRepositoryOrderByCommitDateDesc(Repository repository, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Commit c WHERE c.repository.user = :user AND c.repository.selected = true")
    Long countCommitsByUser(@Param("user") User user);

    @Query("SELECT COUNT(c) FROM Commit c WHERE c.repository.user = :user AND c.repository.selected = true AND c.commitDate > :dateTime")
    Long countCommitsByUserSince(@Param("user") User user, @Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT c.repository.name FROM Commit c " +
           "WHERE c.repository.user = :user AND c.repository.selected = true " +
           "GROUP BY c.repository.id, c.repository.name " +
           "ORDER BY COUNT(c) DESC, c.repository.name ASC")
    List<String> findMostActiveRepositoryName(@Param("user") User user, Pageable pageable);

    @Query("SELECT new com.example.gitpilot.dashboard.dto.ContributorResponse(c.authorName, COUNT(c)) " +
           "FROM Commit c WHERE c.repository = :repository " +
           "GROUP BY c.authorName " +
           "ORDER BY COUNT(c) DESC")
    List<ContributorResponse> findContributorsByRepository(@Param("repository") Repository repository);
}
