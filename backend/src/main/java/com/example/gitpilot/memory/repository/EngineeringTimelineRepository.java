package com.example.gitpilot.memory.repository;

import com.example.gitpilot.memory.entity.EngineeringTimeline;
import com.example.gitpilot.repository.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@org.springframework.stereotype.Repository
public interface EngineeringTimelineRepository extends JpaRepository<EngineeringTimeline, Long> {
    List<EngineeringTimeline> findByRepositoryOrderByCreatedAtDesc(Repository repository);
}
