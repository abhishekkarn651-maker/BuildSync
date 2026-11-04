package com.buildsync.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.buildsync.ratelimit.RateLimitFilter;

import java.util.Arrays;

/**
 * Spring Security configuration enforcing stateless JWT authentication,
 * CORS headers, CSRF protections for APIs, and explicit role-based authorization.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final String allowedOrigins;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(
            @Value("${buildsync.cors.allowed-origins:http://localhost:3000}") String allowedOrigins,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler,
            RateLimitFilter rateLimitFilter) {
        this.allowedOrigins = allowedOrigins;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public Authentication Endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        // Public Health & System Endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // OpenAPI / Swagger Documentation
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // Protected Auth Endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        // Public Skills Catalogue Search
                        .requestMatchers(HttpMethod.GET, "/api/v1/skills/**").permitAll()
                        // Public Media & Avatars
                        .requestMatchers("/api/v1/media/**").permitAll()
                        // Developer Profile Sub-Resources (Skills, Education, Experience) & Self Management
                        .requestMatchers("/api/v1/profiles/me/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/profiles/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/profiles/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/profiles").authenticated()
                        // Public Developer Profiles & Discovery Search
                        .requestMatchers(HttpMethod.GET, "/api/v1/profiles", "/api/v1/profiles/**").permitAll()
                        // Project Management & Collaboration Authenticated Endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects/mine").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects/*/pending-requests").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/projects/*/join-requests").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/projects/*/invitations").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/projects/*/leave").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/projects/*/members/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/projects").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/projects/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/projects/**").authenticated()
                        // WebSocket Handshake Endpoint (STOMP frames are authenticated via ChannelInterceptor)
                        .requestMatchers("/ws/**", "/ws").permitAll()
                        // Messaging & Conversation Endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects/*/conversation").authenticated()
                        .requestMatchers("/api/v1/conversations/**").authenticated()
                        // Public Project Discovery, Details & Members (Public/Private handled at domain/controller level)
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects", "/api/v1/projects/**").permitAll()
                        // Collaboration Dashboard Endpoints
                        .requestMatchers("/api/v1/collaborations/**").authenticated()
                        // GitHub Integration Endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/integrations/github/callback").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/integrations/github/showcase/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects/*/github-repositories").permitAll()
                        .requestMatchers("/api/v1/integrations/github/**").authenticated()
                        .requestMatchers("/api/v1/projects/*/github-repositories/**").authenticated()
                        // All other endpoints require authentication by default
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix(""); // Roles already carry the "ROLE_" prefix

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        String[] origins = allowedOrigins.split(",");
        configuration.setAllowedOrigins(Arrays.asList(origins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setExposedHeaders(Arrays.asList("Set-Cookie", "Authorization", "Retry-After", "RateLimit-Limit", "RateLimit-Remaining", "RateLimit-Reset"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

