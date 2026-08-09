package com.example.gitpilot.memory.service;

import com.example.gitpilot.memory.dto.KnowledgeMapDto;
import com.example.gitpilot.memory.entity.KnowledgeEdge;
import com.example.gitpilot.memory.entity.KnowledgeNode;
import com.example.gitpilot.memory.repository.KnowledgeEdgeRepository;
import com.example.gitpilot.memory.repository.KnowledgeNodeRepository;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeGraphService {

    private final RepositoryRepository repositoryRepository;
    private final KnowledgeNodeRepository nodeRepository;
    private final KnowledgeEdgeRepository edgeRepository;

    public KnowledgeGraphService(RepositoryRepository repositoryRepository,
                                 KnowledgeNodeRepository nodeRepository,
                                 KnowledgeEdgeRepository edgeRepository) {
        this.repositoryRepository = repositoryRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    @Transactional
    public KnowledgeMapDto getKnowledgeMap(Long repositoryId) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseGet(() -> repositoryRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found: " + repositoryId)));

        List<KnowledgeNode> nodes = nodeRepository.findByRepository(repository);
        List<KnowledgeEdge> edges = edgeRepository.findByRepository(repository);

        if (nodes.isEmpty()) {
            seedKnowledgeGraph(repository);
            nodes = nodeRepository.findByRepository(repository);
            edges = edgeRepository.findByRepository(repository);
        }

        List<KnowledgeMapDto.NodeDto> nodeDtos = nodes.stream()
                .map(n -> KnowledgeMapDto.NodeDto.builder()
                        .id(n.getId())
                        .nodeKey(n.getNodeKey())
                        .name(n.getName())
                        .category(n.getCategory())
                        .description(n.getDescription())
                        .complexityScore(n.getComplexityScore())
                        .build())
                .collect(Collectors.toList());

        List<KnowledgeMapDto.EdgeDto> edgeDtos = edges.stream()
                .map(e -> KnowledgeMapDto.EdgeDto.builder()
                        .id(e.getId())
                        .sourceNodeKey(e.getSourceNodeKey())
                        .targetNodeKey(e.getTargetNodeKey())
                        .relationshipType(e.getRelationshipType())
                        .label(e.getLabel())
                        .build())
                .collect(Collectors.toList());

        return KnowledgeMapDto.builder()
                .repositoryId(repository.getId())
                .nodes(nodeDtos)
                .edges(edgeDtos)
                .build();
    }

    @Transactional
    public void seedKnowledgeGraph(Repository repository) {
        List<KnowledgeNode> nodes = new ArrayList<>();
        nodes.add(createNode(repository, "AUTH", "Authentication Module", "SECURITY", "Handles OAuth2 login, Spring Security filter chains, and user principal context.", 80));
        nodes.add(createNode(repository, "OAUTH2", "GitHub OAuth2 Gateway", "INTEGRATION", "Manages GitHub authorization tokens and authorized client sessions.", 70));
        nodes.add(createNode(repository, "SECURITY_CFG", "SecurityConfig Filter", "SECURITY", "Configures CORS headers, stateless sessions, and public endpoint permissions.", 65));
        nodes.add(createNode(repository, "USER_SVC", "UserService Domain", "CORE", "Synchronizes user profile attributes from GitHub payload into PostgreSQL.", 60));
        nodes.add(createNode(repository, "REPO_SYNC", "Repository Synchronization", "CORE", "Fetches GitHub repositories, branches, and indexes commit metadata.", 85));
        nodes.add(createNode(repository, "WEBHOOK", "Webhook Handler Pipeline", "EVENT", "Processes GitHub push events using Strategy pattern and HMAC signature validation.", 90));
        nodes.add(createNode(repository, "INSIGHTS", "AI Insights & Memory Engine", "AI", "Generates Repository DNA, Journey Timelines, Health Scores, and Onboarding Roadmaps.", 95));
        nodes.add(createNode(repository, "DB_FLYWAY", "Flyway Migration & JPA", "DATABASE", "Database schema versioning, Flyway migrations, and Spring Data JPA repositories.", 75));

        nodeRepository.saveAll(nodes);

        List<KnowledgeEdge> edges = new ArrayList<>();
        edges.add(createEdge(repository, "AUTH", "OAUTH2", "DEPENDS_ON", "Authenticates via"));
        edges.add(createEdge(repository, "AUTH", "SECURITY_CFG", "CONFIGURED_BY", "Protected by"));
        edges.add(createEdge(repository, "OAUTH2", "USER_SVC", "DELEGATES_TO", "Syncs user to"));
        edges.add(createEdge(repository, "USER_SVC", "DB_FLYWAY", "PERSISTS_VIA", "Stored in DB via"));
        edges.add(createEdge(repository, "REPO_SYNC", "OAUTH2", "USES_TOKEN", "Uses GitHub token from"));
        edges.add(createEdge(repository, "WEBHOOK", "REPO_SYNC", "TRIGGERS", "Triggers auto-sync on"));
        edges.add(createEdge(repository, "INSIGHTS", "REPO_SYNC", "ANALYZES", "Analyzes data from"));
        edges.add(createEdge(repository, "INSIGHTS", "DB_FLYWAY", "CACHES_IN", "Caches reports in"));

        edgeRepository.saveAll(edges);
    }

    private KnowledgeNode createNode(Repository repo, String key, String name, String category, String desc, int complexity) {
        KnowledgeNode n = new KnowledgeNode();
        n.setRepository(repo);
        n.setNodeKey(key);
        n.setName(name);
        n.setCategory(category);
        n.setDescription(desc);
        n.setComplexityScore(complexity);
        return n;
    }

    private KnowledgeEdge createEdge(Repository repo, String src, String target, String type, String label) {
        KnowledgeEdge e = new KnowledgeEdge();
        e.setRepository(repo);
        e.setSourceNodeKey(src);
        e.setTargetNodeKey(target);
        e.setRelationshipType(type);
        e.setLabel(label);
        return e;
    }
}
