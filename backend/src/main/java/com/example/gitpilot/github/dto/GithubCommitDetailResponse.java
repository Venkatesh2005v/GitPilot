package com.example.gitpilot.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Captures ONLY the fields of GitHub's single-commit endpoint
 * (GET /repos/{owner}/{repo}/commits/{sha}) that the application needs.
 * Unknown fields are ignored so GitHub payload changes never break parsing.
 * This is a transport DTO — it is never persisted.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubCommitDetailResponse {

    @JsonProperty("sha")
    private String sha;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("commit")
    private CommitDetail commit;

    @JsonProperty("stats")
    private Stats stats;

    @JsonProperty("files")
    private List<File> files;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommitDetail {
        @JsonProperty("message")
        private String message;

        @JsonProperty("author")
        private Author author;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Author {
        @JsonProperty("name")
        private String name;

        @JsonProperty("email")
        private String email;

        @JsonProperty("date")
        private LocalDateTime date;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Stats {
        @JsonProperty("total")
        private Integer total;

        @JsonProperty("additions")
        private Integer additions;

        @JsonProperty("deletions")
        private Integer deletions;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class File {
        @JsonProperty("filename")
        private String filename;

        @JsonProperty("status")
        private String status;

        @JsonProperty("additions")
        private Integer additions;

        @JsonProperty("deletions")
        private Integer deletions;

        @JsonProperty("changes")
        private Integer changes;

        @JsonProperty("patch")
        private String patch;

        @JsonProperty("previous_filename")
        private String previousFilename;
    }
}
