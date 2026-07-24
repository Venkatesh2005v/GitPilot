package com.example.gitpilot.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GithubRepositoryResponse {
    private Long id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("full_name")
    private String fullName;
    @JsonProperty("default_branch")
    private String defaultBranch;
    @JsonProperty("private")
    private Boolean isPrivate;
    @JsonProperty("html_url")
    private String htmlUrl;
}
