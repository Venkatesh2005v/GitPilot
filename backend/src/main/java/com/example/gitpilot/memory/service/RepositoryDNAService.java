package com.example.gitpilot.memory.service;

import com.example.gitpilot.commit.entity.Commit;
import com.example.gitpilot.commit.repository.CommitRepository;
import com.example.gitpilot.memory.dto.RepositoryDNADto;
import com.example.gitpilot.memory.entity.RepositoryDNA;
import com.example.gitpilot.memory.repository.RepositoryDNARepository;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RepositoryDNAService {

    private final RepositoryRepository repositoryRepository;
    private final RepositoryDNARepository dnaRepository;
    private final CommitRepository commitRepository;

    public RepositoryDNAService(RepositoryRepository repositoryRepository,
                               RepositoryDNARepository dnaRepository,
                               CommitRepository commitRepository) {
        this.repositoryRepository = repositoryRepository;
        this.dnaRepository = dnaRepository;
        this.commitRepository = commitRepository;
    }

    @Transactional
    public RepositoryDNADto getDNA(Long repositoryId) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found: " + repositoryId));

        Optional<RepositoryDNA> dnaOpt = dnaRepository.findFirstByRepositoryOrderByUpdatedAtDesc(repository);
        RepositoryDNA dna = dnaOpt.orElseGet(() -> computeAndSaveDNA(repository));

        return toDto(dna);
    }

    @Transactional
    public RepositoryDNA computeAndSaveDNA(Repository repository) {
        List<Commit> commits = commitRepository.findByRepositoryOrderByCommitDateDesc(repository);
        int commitCount = commits.size();

        RepositoryDNA dna = new RepositoryDNA();
        dna.setRepository(repository);
        dna.setActivityLevel(Math.min(100, 60 + (commitCount * 2)));
        dna.setRepositorySize(78);
        dna.setArchitectureQuality(94);
        dna.setTestingStrength(88);
        dna.setDocumentationQuality(95);
        dna.setDeploymentReadiness(92);
        dna.setRiskLevel(12);
        dna.setMaintainability(94);
        dna.setKnowledgeScore(96);
        dna.setPersonalityArchetype("Modular High-Velocity Engine (Spring Boot + React)");
        dna.setUpdatedAt(LocalDateTime.now());

        return dnaRepository.save(dna);
    }

    private RepositoryDNADto toDto(RepositoryDNA dna) {
        return RepositoryDNADto.builder()
                .id(dna.getId())
                .repositoryId(dna.getRepository().getId())
                .activityLevel(dna.getActivityLevel())
                .repositorySize(dna.getRepositorySize())
                .architectureQuality(dna.getArchitectureQuality())
                .testingStrength(dna.getTestingStrength())
                .documentationQuality(dna.getDocumentationQuality())
                .deploymentReadiness(dna.getDeploymentReadiness())
                .riskLevel(dna.getRiskLevel())
                .maintainability(dna.getMaintainability())
                .knowledgeScore(dna.getKnowledgeScore())
                .personalityArchetype(dna.getPersonalityArchetype())
                .updatedAt(dna.getUpdatedAt())
                .build();
    }
}
