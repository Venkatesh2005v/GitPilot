package com.example.gitpilot.memory.service;

import com.example.gitpilot.memory.dto.KnowledgeMapDto;
import com.example.gitpilot.memory.entity.KnowledgeEdge;
import com.example.gitpilot.memory.entity.KnowledgeNode;
import com.example.gitpilot.memory.repository.KnowledgeEdgeRepository;
import com.example.gitpilot.memory.repository.KnowledgeNodeRepository;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphServiceTest {

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private KnowledgeNodeRepository nodeRepository;

    @Mock
    private KnowledgeEdgeRepository edgeRepository;

    @InjectMocks
    private KnowledgeGraphService knowledgeGraphService;

    private Repository mockRepo;

    @BeforeEach
    void setUp() {
        mockRepo = new Repository();
        mockRepo.setId(1L);
        mockRepo.setName("test-repo");
    }

    @Test
    void testGetKnowledgeMapExisting() {
        KnowledgeNode node = new KnowledgeNode();
        node.setId(10L);
        node.setRepository(mockRepo);
        node.setNodeKey("AUTH");
        node.setName("Auth Module");

        KnowledgeEdge edge = new KnowledgeEdge();
        edge.setId(20L);
        edge.setRepository(mockRepo);
        edge.setSourceNodeKey("AUTH");
        edge.setTargetNodeKey("OAUTH");

        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(mockRepo));
        when(nodeRepository.findByRepository(mockRepo)).thenReturn(List.of(node));
        when(edgeRepository.findByRepository(mockRepo)).thenReturn(List.of(edge));

        KnowledgeMapDto map = knowledgeGraphService.getKnowledgeMap(1L);

        assertNotNull(map);
        assertEquals(1, map.getNodes().size());
        assertEquals("AUTH", map.getNodes().get(0).getNodeKey());
        assertEquals(1, map.getEdges().size());
    }
}
