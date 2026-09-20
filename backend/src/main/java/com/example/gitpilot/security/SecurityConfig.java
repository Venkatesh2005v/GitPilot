package com.example.gitpilot.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    /**
     * Comma-separated list of allowed browser origins for cross-origin requests.
     * Local development leaves this empty (frontend is same-origin via the Vite proxy),
     * so no cross-origin access is granted and behavior is unchanged.
     * Production sets APP_CORS_ALLOWED_ORIGINS to the Vercel frontend URL.
     */
    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    /**
     * Where to send the browser after a successful OAuth2 login. In the split deployment this is
     * the Vercel frontend URL (set via OAUTH2_SUCCESS_REDIRECT_URI). Defaults to "/" so local
     * development (same-origin) behavior is unchanged.
     */
    @Value("${app.oauth2.success-redirect-uri:/}")
    private String frontendUrl;

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

    /**
     * CORS configuration source. When app.cors.allowed-origins is empty (local dev),
     * no origins are registered and cross-origin requests are simply not granted —
     * identical to the previous behavior. In production the Vercel origin is allowed
     * with credentials so the session cookie can flow from the frontend to the backend.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (!origins.isEmpty()) {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(origins);
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setAllowCredentials(true); // required: app uses HttpSession + OAuth2AuthorizedClientRepository
            source.registerCorsConfiguration("/**", config);
        }
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            OAuth2AuthorizedClientRepository authorizedClientRepository) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                        .defaultSuccessUrl(frontendUrl, true)
                );
        return http.build();
    }
}
