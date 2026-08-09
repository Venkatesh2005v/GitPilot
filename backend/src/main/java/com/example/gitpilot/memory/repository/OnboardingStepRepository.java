package com.example.gitpilot.memory.repository;

import com.example.gitpilot.memory.entity.OnboardingStep;
import com.example.gitpilot.repository.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@org.springframework.stereotype.Repository
public interface OnboardingStepRepository extends JpaRepository<OnboardingStep, Long> {
    List<OnboardingStep> findByRepositoryOrderByStepOrderAsc(Repository repository);
}
