package com.example.gitpilot.memory.repository;

import com.example.gitpilot.memory.entity.Recommendation;
import com.example.gitpilot.repository.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@org.springframework.stereotype.Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByRepository(Repository repository);
}
