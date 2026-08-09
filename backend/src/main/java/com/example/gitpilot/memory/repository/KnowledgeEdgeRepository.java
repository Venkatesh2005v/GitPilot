package com.example.gitpilot.memory.repository;

import com.example.gitpilot.memory.entity.KnowledgeEdge;
import com.example.gitpilot.repository.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@org.springframework.stereotype.Repository
public interface KnowledgeEdgeRepository extends JpaRepository<KnowledgeEdge, Long> {
    List<KnowledgeEdge> findByRepository(Repository repository);
    List<KnowledgeEdge> findByRepositoryId(Long repositoryId);
    List<KnowledgeEdge> findByRepositoryAndSourceNodeKey(Repository repository, String sourceNodeKey);
    List<KnowledgeEdge> findByRepositoryIdAndSourceNodeKey(Long repositoryId, String sourceNodeKey);
    List<KnowledgeEdge> findByRepositoryAndTargetNodeKey(Repository repository, String targetNodeKey);
    List<KnowledgeEdge> findByRepositoryIdAndTargetNodeKey(Long repositoryId, String targetNodeKey);
    void deleteByRepository(Repository repository);
}
