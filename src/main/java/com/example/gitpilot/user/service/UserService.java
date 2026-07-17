package com.example.gitpilot.user.service;

import com.example.gitpilot.user.dto.UserResponse;
import com.example.gitpilot.user.entity.User;
import com.example.gitpilot.user.repository.UserRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public UserResponse synchronizeUser(OAuth2User oauthUser){
        Map<String, Object> attributes = oauthUser.getAttributes();
        Long githubId = ((Number) attributes.get("id")).longValue();
        String username = (String) attributes.get("login");
        String name = (String) attributes.get("name");
        String email = (String) attributes.get("email");
        String avatarUrl = (String) attributes.get("avatar_url");
        String profileUrl = (String) attributes.get("html_url");

        Optional<User> optionalUser =
                userRepository.findByGithubId(githubId);

        User user = optionalUser.orElseGet(User::new);
        user.setGithubId(githubId);
        user.setUsername(username);
        user.setName(name);
        user.setEmail(email);
        user.setAvatarUrl(avatarUrl);
        user.setProfileUrl(profileUrl);

        User savedUser = userRepository.save(user);
        return new UserResponse(
                savedUser.getUsername(),
                savedUser.getName(),
                savedUser.getAvatarUrl(),
                savedUser.getProfileUrl()
        );
    }
}
