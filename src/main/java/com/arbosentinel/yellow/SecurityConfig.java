package com.arbosentinel.yellow;

// ================================================
// YELLOW layer — Spring Security configuration
// Strategy: public platform by design — all GET data
// endpoints are open. JWT guards only ETL triggers
// and any future admin operations.
// Spring Boot 3 / Spring Security 6 API (no adapter)
// ================================================

import com.arbosentinel.white.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ── Public endpoints — all GET data is open ──────────────
                .requestMatchers(HttpMethod.GET,
                    "/api/diseases/**",
                    "/api/dengue/**",
                    "/api/malaria/**",
                    "/api/westnile/**",
                    "/api/zika/**",
                    "/api/sinan/**",
                    "/api/pharmacology/**",
                    "/api/alerts/**",
                    "/api/risk/**",
                    "/api/dashboard/**",
                    "/api/ml/predictions/**"
                ).permitAll()
                // ── ML prediction — public (research demo, not clinical) ──
                .requestMatchers(HttpMethod.POST, "/api/ml/run/dengue").permitAll()
                // ── Health check — public ────────────────────────────────
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // ── ETL triggers + admin — requires JWT ──────────────────
                .requestMatchers("/api/admin/**", "/api/ml/run/**", "/api/etl/**")
                    .authenticated()
                // Everything else defaults to authenticated
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Allow React dev server + Railway deployed frontend
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:5175",
            "https://arbosentinel.vercel.app",   // Production frontend
            "https://*.vercel.app",               // Vercel preview deployments
            "https://*.railway.app",
            "https://*.up.railway.app"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
