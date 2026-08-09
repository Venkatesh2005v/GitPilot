package com.example.gitpilot.webhook.repository;

import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.webhook.entity.RepositoryWebhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepositoryWebhookRepository extends JpaRepository<RepositoryWebhook, Long> {
    Optional<RepositoryWebhook> findByRepository(Repository repository);
    Optional<RepositoryWebhook> findByRepositoryId(Long repositoryId);
}
