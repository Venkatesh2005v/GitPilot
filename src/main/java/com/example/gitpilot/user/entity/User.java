package com.example.gitpilot.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long githubId;

    @Column(unique = true, nullable = false)
    private String username;

    private String name;

    private String email;

    @Column(nullable = false)
    private String avatarUrl;

    @Column(nullable = false)
    private String profileUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
