package com.example.gitpilot.memory.repository;

import com.example.gitpilot.memory.entity.KnowledgeNode;
import com.example.gitpilot.repository.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface KnowledgeNodeRepository extends JpaRepository<KnowledgeNode, Long> {
    List<KnowledgeNode> findByRepository(Repository repository);
    List<KnowledgeNode> findByRepositoryId(Long repositoryId);
    Optional<KnowledgeNode> findByRepositoryAndNodeKey(Repository repository, String nodeKey);
    Optional<KnowledgeNode> findByRepositoryIdAndNodeKey(Long repositoryId, String nodeKey);
    List<KnowledgeNode> findByRepositoryAndCategory(Repository repository, String category);
    List<KnowledgeNode> findByRepositoryIdAndCategory(Long repositoryId, String category);
    void deleteByRepository(Repository repository);
}
