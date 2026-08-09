package com.example.gitpilot.memory.repository;

import com.example.gitpilot.memory.entity.EngineeringDecision;
import com.example.gitpilot.repository.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@org.springframework.stereotype.Repository
public interface EngineeringDecisionRepository extends JpaRepository<EngineeringDecision, Long> {
    List<EngineeringDecision> findByRepositoryOrderByDateInferredDesc(Repository repository);
}
