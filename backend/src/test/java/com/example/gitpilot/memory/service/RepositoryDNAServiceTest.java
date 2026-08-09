package com.example.gitpilot.memory.service;

import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.memory.dto.RepositoryDNADto;
import com.example.gitpilot.memory.entity.RepositoryDNA;
import com.example.gitpilot.memory.repository.RepositoryDNARepository;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryDNAServiceTest {

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private RepositoryDNARepository dnaRepository;

    @Mock
    private CommitRepository commitRepository;

    @InjectMocks
    private RepositoryDNAService dnaService;

    private Repository mockRepo;

    @BeforeEach
    void setUp() {
        mockRepo = new Repository();
        mockRepo.setId(1L);
        mockRepo.setName("test-repo");
    }

    @Test
    void testGetDNAExisting() {
        RepositoryDNA dna = new RepositoryDNA();
        dna.setId(1L);
        dna.setRepository(mockRepo);
        dna.setActivityLevel(90);
        dna.setArchitectureQuality(95);

        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(mockRepo));
        when(dnaRepository.findFirstByRepositoryOrderByUpdatedAtDesc(mockRepo)).thenReturn(Optional.of(dna));

        RepositoryDNADto result = dnaService.getDNA(1L);

        assertNotNull(result);
        assertEquals(90, result.getActivityLevel());
        assertEquals(95, result.getArchitectureQuality());
    }

    @Test
    void testGetDNAComputesWhenMissing() {
        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(mockRepo));
        when(dnaRepository.findFirstByRepositoryOrderByUpdatedAtDesc(mockRepo)).thenReturn(Optional.empty());
        when(commitRepository.findByRepositoryOrderByCommitDateDesc(mockRepo)).thenReturn(Collections.emptyList());
        when(dnaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RepositoryDNADto result = dnaService.getDNA(1L);

        assertNotNull(result);
        assertNotNull(result.getPersonalityArchetype());
        verify(dnaRepository, times(1)).save(any());
    }
}
