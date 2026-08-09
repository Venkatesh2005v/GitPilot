package com.example.gitpilot.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Centralized GitHub access-token resolver.
 * All controllers delegate here. Reads from the session-backed repository.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubTokenResolver {

    private final OAuth2AuthorizedClientRepository authorizedClientRepository;

    /**
     * Resolve the GitHub access token from the current session.
     * Returns null if not authenticated or token unavailable.
     */
    public String resolve(Authentication authentication, HttpServletRequest request) {
        log.info("[GitHubAuth] authenticationPresent={} oauth2={}", authentication != null,
                authentication instanceof OAuth2AuthenticationToken);

        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            return null;
        }

        String registrationId = token.getAuthorizedClientRegistrationId();
        log.info("[GitHubAuth] registrationId={} principalName={}", registrationId, token.getName());

        OAuth2AuthorizedClient client = authorizedClientRepository.loadAuthorizedClient(
                registrationId, authentication, request);

        boolean hasToken = client != null && client.getAccessToken() != null;
        log.info("[GitHubAuth] authorizedClientPresent={} accessTokenPresent={}", client != null, hasToken);

        return hasToken ? client.getAccessToken().getTokenValue() : null;
    }

    /**
     * Get the full OAuth2AuthorizedClient (for services that need it).
     */
    public OAuth2AuthorizedClient getClient(Authentication authentication, HttpServletRequest request) {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            return null;
        }
        return authorizedClientRepository.loadAuthorizedClient(
                token.getAuthorizedClientRegistrationId(), authentication, request);
    }
}
