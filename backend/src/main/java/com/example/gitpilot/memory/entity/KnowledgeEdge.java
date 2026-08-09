package com.example.gitpilot.memory.entity;

import com.example.gitpilot.repository.entity.Repository;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "knowledge_edges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "source_node_key", nullable = false)
    private String sourceNodeKey;

    @Column(name = "target_node_key", nullable = false)
    private String targetNodeKey;

    @Column(name = "relationship_type")
    private String relationshipType;

    private String label;
}
