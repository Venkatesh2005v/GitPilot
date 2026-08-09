package com.example.gitpilot.memory.service;

import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.memory.dto.RepositoryJourneyDto;
import com.example.gitpilot.memory.entity.RepositoryJourney;
import com.example.gitpilot.memory.repository.RepositoryJourneyRepository;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryJourneyServiceTest {

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private RepositoryJourneyRepository journeyRepository;

    @Mock
    private CommitRepository commitRepository;

    @InjectMocks
    private RepositoryJourneyService journeyService;

    private Repository mockRepo;

    @BeforeEach
    void setUp() {
        mockRepo = new Repository();
        mockRepo.setId(1L);
        mockRepo.setName("test-repo");
    }

    @Test
    void testGetJourneyExisting() {
        RepositoryJourney j = new RepositoryJourney();
        j.setId(10L);
        j.setRepository(mockRepo);
        j.setMilestoneName("Test Milestone");
        j.setEventDate(LocalDateTime.now());
        j.setMonthVal("July");
        j.setYearVal(2026);

        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(mockRepo));
        when(journeyRepository.findByRepositoryOrderByEventDateDesc(mockRepo)).thenReturn(List.of(j));

        List<RepositoryJourneyDto> result = journeyService.getJourney(1L, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Milestone", result.get(0).getMilestoneName());
    }

    @Test
    void testGetJourneySeedsWhenEmpty() {
        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(mockRepo));
        when(journeyRepository.findByRepositoryOrderByEventDateDesc(mockRepo)).thenReturn(Collections.emptyList());
        when(commitRepository.findByRepositoryOrderByCommitDateDesc(mockRepo)).thenReturn(Collections.emptyList());
        when(journeyRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<RepositoryJourneyDto> result = journeyService.getJourney(1L, null, null, null, null);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(journeyRepository, times(1)).saveAll(any());
    }
}
