package com.example.gitpilot.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {

    /**
     * Use HttpSession-backed authorized client repository.
     * This ensures the OAuth2AuthorizedClient (with access token) is stored
     * in the same HTTP session as the OAuth2AuthenticationToken.
     * Both survive together and both are lost together on session expiry.
     */
    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    /**
     * Provide an OAuth2AuthorizedClientManager that controllers can use
     * to load authorized clients from the session-backed repository.
     */
    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {
        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build();
        DefaultOAuth2AuthorizedClientManager manager =
                new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            OAuth2AuthorizedClientRepository authorizedClientRepository) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/assets/**",
                                "/static/**",
                                "/favicon.ico",
                                "/webhooks/github",
                                "/me",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/actuator/**",
                                "/dashboard",
                                "/onboarding",
                                "/architecture",
                                "/team-intelligence",
                                "/recommendations",
                                "/analytics",
                                "/insights",
                                "/settings",
                                "/repositories/*"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                request -> {
                                    String uri = request.getRequestURI();
                                    return uri.startsWith("/api/") || uri.startsWith("/ai/")
                                            || uri.startsWith("/architecture/") || uri.startsWith("/dashboard/")
                                            || uri.startsWith("/repositories/") || uri.startsWith("/github/");
                                }
                        )
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizedClientRepository(authorizedClientRepository)
                );
        return http.build();
    }
}
