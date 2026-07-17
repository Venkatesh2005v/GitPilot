package com.example.gitpilot.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GithubCommitResponse {

    @JsonProperty("sha")
    private String sha;

    @JsonProperty("commit")
    private CommitDetail commit;

    @JsonProperty("html_url")
    private String htmlUrl;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CommitDetail {
        @JsonProperty("message")
        private String message;

        @JsonProperty("author")
        private AuthorDetail author;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthorDetail {
        @JsonProperty("name")
        private String name;

        @JsonProperty("email")
        private String email;

        @JsonProperty("date")
        private LocalDateTime date;
    }
}
