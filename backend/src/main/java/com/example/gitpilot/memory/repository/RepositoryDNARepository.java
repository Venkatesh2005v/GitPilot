package com.example.gitpilot.memory.repository;

import com.example.gitpilot.memory.entity.RepositoryDNA;
import com.example.gitpilot.repository.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface RepositoryDNARepository extends JpaRepository<RepositoryDNA, Long> {
    Optional<RepositoryDNA> findByRepository(Repository repository);
    Optional<RepositoryDNA> findFirstByRepositoryOrderByUpdatedAtDesc(Repository repository);
}
