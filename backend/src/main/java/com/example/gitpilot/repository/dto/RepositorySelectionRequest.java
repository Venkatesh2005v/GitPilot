package com.example.gitpilot.repository.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RepositorySelectionRequest {
    @NotNull(message = "GitHub repository IDs list cannot be null")
    private List<Long> githubRepositoryIds;
}
