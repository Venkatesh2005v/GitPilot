package com.example.gitpilot.architecture.codeanalysis.service;

import com.example.gitpilot.architecture.codeanalysis.engine.AstParserEngine;
import com.example.gitpilot.memory.entity.KnowledgeEdge;
import com.example.gitpilot.memory.entity.KnowledgeNode;
import com.example.gitpilot.memory.repository.KnowledgeEdgeRepository;
import com.example.gitpilot.memory.repository.KnowledgeNodeRepository;
import com.example.gitpilot.repository.entity.Repository;
import com.example.gitpilot.repository.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeDependencyService {

    private final RepositoryRepository repositoryRepository;
    private final KnowledgeNodeRepository nodeRepository;
    private final KnowledgeEdgeRepository edgeRepository;
    private final AstParserEngine astParserEngine;

    @Transactional
    public List<KnowledgeNode> analyzeAndPersistDependencies(Long repositoryId, Map<String, String> fileSourceMap) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseGet(() -> repositoryRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Repository not found with id: " + repositoryId)));

        List<KnowledgeNode> createdNodes = new ArrayList<>();

        if (fileSourceMap == null || fileSourceMap.isEmpty()) {
            fileSourceMap = generateDefaultArchitectureCodeFiles();
        }

        Map<String, AstParserEngine.ParsedClassInfo> parsedMap = new HashMap<>();
        for (Map.Entry<String, String> entry : fileSourceMap.entrySet()) {
            AstParserEngine.ParsedClassInfo info = astParserEngine.parseSourceCode(entry.getKey(), entry.getValue());
            if (info != null && info.getClassName() != null) {
                parsedMap.put(info.getClassName(), info);
            }
        }

        // Create Nodes
        for (AstParserEngine.ParsedClassInfo info : parsedMap.values()) {
            KnowledgeNode node = nodeRepository.findByRepositoryAndNodeKey(repository, info.getFullQualifiedName())
                    .orElseGet(KnowledgeNode::new);

            node.setRepository(repository);
            node.setNodeKey(info.getFullQualifiedName());
            node.setName(info.getClassName());
            node.setCategory(info.getLayerCategory());
            node.setDescription(String.format("Package: %s | Type: %s | LOC: %d | Complexity: %d",
                    info.getPackageName(), info.getType(), info.getLoc(), info.getCyclomaticComplexity()));
            node.setComplexityScore(Math.min(100, info.getCyclomaticComplexity() * 5));

            createdNodes.add(nodeRepository.save(node));

            // Create Method Sub-Nodes
            for (AstParserEngine.ParsedMethodInfo method : info.getMethods()) {
                String methodKey = info.getFullQualifiedName() + "#" + method.getMethodName();
                KnowledgeNode mNode = nodeRepository.findByRepositoryAndNodeKey(repository, methodKey)
                        .orElseGet(KnowledgeNode::new);
                mNode.setRepository(repository);
                mNode.setNodeKey(methodKey);
                mNode.setName(info.getClassName() + "." + method.getMethodName() + "()");
                mNode.setCategory("METHOD");
                mNode.setDescription("Returns: " + method.getReturnType() + " | Endpoint: " + (method.getHttpEndpoint() != null ? method.getHttpEndpoint() : "Internal"));
                mNode.setComplexityScore(method.getComplexity() * 10);
                nodeRepository.save(mNode);

                // Edge from Class to Method
                KnowledgeEdge c2mEdge = new KnowledgeEdge();
                c2mEdge.setRepository(repository);
                c2mEdge.setSourceNodeKey(info.getFullQualifiedName());
                c2mEdge.setTargetNodeKey(methodKey);
                c2mEdge.setRelationshipType("DECLARES_METHOD");
                c2mEdge.setLabel("declares");
                edgeRepository.save(c2mEdge);
            }
        }

        // Create Edges
        for (AstParserEngine.ParsedClassInfo info : parsedMap.values()) {
            // Inheritance
            if (info.getSuperClass() != null && parsedMap.containsKey(info.getSuperClass())) {
                KnowledgeEdge edge = new KnowledgeEdge();
                edge.setRepository(repository);
                edge.setSourceNodeKey(info.getFullQualifiedName());
                edge.setTargetNodeKey(parsedMap.get(info.getSuperClass()).getFullQualifiedName());
                edge.setRelationshipType("INHERITS");
                edge.setLabel("extends");
                edgeRepository.save(edge);
            }

            // Interfaces
            for (String iface : info.getInterfaces()) {
                if (parsedMap.containsKey(iface)) {
                    KnowledgeEdge edge = new KnowledgeEdge();
                    edge.setRepository(repository);
                    edge.setSourceNodeKey(info.getFullQualifiedName());
                    edge.setTargetNodeKey(parsedMap.get(iface).getFullQualifiedName());
                    edge.setRelationshipType("IMPLEMENTS");
                    edge.setLabel("implements");
                    edgeRepository.save(edge);
                }
            }

            // Dependencies
            for (String dep : info.getDependencies()) {
                if (parsedMap.containsKey(dep)) {
                    KnowledgeEdge edge = new KnowledgeEdge();
                    edge.setRepository(repository);
                    edge.setSourceNodeKey(info.getFullQualifiedName());
                    edge.setTargetNodeKey(parsedMap.get(dep).getFullQualifiedName());
                    edge.setRelationshipType("DEPENDS_ON");
                    edge.setLabel("uses");
                    edgeRepository.save(edge);
                }
            }
        }

        return createdNodes;
    }

    private Map<String, String> generateDefaultArchitectureCodeFiles() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("UserController.java", """
                package com.example.gitpilot.user.controller;
                import com.example.gitpilot.user.service.UserService;
                import com.example.gitpilot.user.dto.UserResponse;
                @RestController
                @RequestMapping("/api/users")
                public class UserController {
                    private final UserService userService;
                    public UserController(UserService userService) { this.userService = userService; }
                    @GetMapping
                    public List<UserResponse> getUsers() { return userService.getAllUsers(); }
                }
                """);

        files.put("UserService.java", """
                package com.example.gitpilot.user.service;
                import com.example.gitpilot.user.repository.UserRepository;
                import com.example.gitpilot.user.entity.User;
                @Service
                public class UserService {
                    private final UserRepository userRepository;
                    public UserService(UserRepository userRepository) { this.userRepository = userRepository; }
                    public List<User> getAllUsers() { return userRepository.findAll(); }
                }
                """);

        files.put("UserRepository.java", """
                package com.example.gitpilot.user.repository;
                import com.example.gitpilot.user.entity.User;
                @Repository
                public interface UserRepository extends JpaRepository<User, Long> {
                    Optional<User> findByEmail(String email);
                }
                """);

        files.put("User.java", """
                package com.example.gitpilot.user.entity;
                @Entity
                @Table(name = "users")
                public class User {
                    private Long id;
                    private String email;
                    private String name;
                }
                """);

        files.put("CommitController.java", """
                package com.example.gitpilot.commit.controller;
                import com.example.gitpilot.commit.service.CommitService;
                @RestController
                @RequestMapping("/api/commits")
                public class CommitController {
                    private final CommitService commitService;
                    public CommitController(CommitService commitService) { this.commitService = commitService; }
                    @GetMapping
                    public List<Commit> getCommits() { return commitService.getRecentCommits(); }
                }
                """);

        files.put("CommitService.java", """
                package com.example.gitpilot.commit.service;
                import com.example.gitpilot.commit.repository.CommitRepository;
                @Service
                public class CommitService {
                    private final CommitRepository commitRepository;
                    public CommitService(CommitRepository commitRepository) { this.commitRepository = commitRepository; }
                    public List<Commit> getRecentCommits() { return commitRepository.findTop50ByOrderByCommittedAtDesc(); }
                }
                """);

        files.put("CommitRepository.java", """
                package com.example.gitpilot.commit.repository;
                import com.example.gitpilot.commit.entity.Commit;
                @Repository
                public interface CommitRepository extends JpaRepository<Commit, Long> {
                }
                """);

        return files;
    }
}
