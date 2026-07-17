package com.example.gitpilot.repository.repository;

import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepositoryRepository extends JpaRepository<Repository, Long> {
    Optional<Repository> findByGithubRepoId(Long githubRepoId);
    List<Repository> findByUser(User user);
}
