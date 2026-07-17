package com.example.gitpilot.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RepositoryResponse {
    private Long githubRepoId;
    private String name;
    private String defaultBranch;
    private String htmlUrl;
    private Boolean privateRepo;
    private Boolean selected;
}
