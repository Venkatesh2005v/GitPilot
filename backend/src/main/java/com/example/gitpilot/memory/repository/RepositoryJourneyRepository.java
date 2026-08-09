package com.example.gitpilot.memory.repository;

import com.example.gitpilot.memory.entity.RepositoryJourney;
import com.example.gitpilot.repository.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface RepositoryJourneyRepository extends JpaRepository<RepositoryJourney, Long> {
    Optional<RepositoryJourney> findByRepository(Repository repository);
    List<RepositoryJourney> findByRepositoryOrderByEventDateDesc(Repository repository);
}
