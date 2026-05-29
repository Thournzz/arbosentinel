package com.arbosentinel.yellow;

// ══════════════════════════════════════════════════════════════════════════════
// YELLOW layer — WebClient configuration
// Configures reactive HTTP clients for two external services:
//   1. mlServiceClient     → Python FastAPI ML microservice (port 8000)
//   2. pahoApiClient       → CMU Delphi Epidata API (api.delphi.cmu.edu)
//
// LEARNING NOTE — Why WebClient instead of RestTemplate?
//   RestTemplate is the older Spring HTTP client — it is BLOCKING (thread waits
//   for the response before doing anything else). WebClient is REACTIVE — it can
//   issue the HTTP request and do other work while waiting for the reply.
//   Even in a non-reactive Spring MVC app, WebClient is preferred for external
//   HTTP calls because it doesn't tie up a thread for the full round trip.
//   Spring recommends WebClient for all new code; RestTemplate is in maintenance mode.
//
// LEARNING NOTE — Why separate beans for ML and PAHO?
//   Each WebClient bean has its own baseUrl, so callers just write:
//     pahoApiClient.get().uri("/epidata/paho_dengue/")  (relative URL)
//   rather than:
//     someClient.get().uri("https://api.delphi.cmu.edu/epidata/paho_dengue/")
//   Separating them also means we can tune timeouts, headers, and codecs
//   independently — the ML service and PAHO have different performance profiles.
//
// LEARNING NOTE — @Value and the colon default syntax:
//   @Value("${arbosentinel.ml.service.url:http://localhost:8000}")
//   reads the property "arbosentinel.ml.service.url" from application.properties.
//   The :http://localhost:8000 part is a DEFAULT — if the property is not set,
//   Spring uses localhost:8000. In prod (Railway), the env var overrides it.
//   This is why we never hardcode URLs in code — one change in application.yml
//   switches both dev and prod without touching Java.
// ══════════════════════════════════════════════════════════════════════════════

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${arbosentinel.ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    // LEARNING NOTE — Property with hardcoded default for public APIs:
    //   The CMU Delphi API is a public endpoint — no API key, no auth.
    //   The URL is stable and unlikely to change, so the hardcoded default is safe.
    //   We still read it from config so we can override it in tests (e.g. point to
    //   a WireMock stub) without changing the production code.
    @Value("${paho.delphi.api.url:https://api.delphi.cmu.edu}")
    private String pahoApiUrl;

    // ── Bean 1: ML microservice ────────────────────────────────────────────────

    @Bean
    public WebClient mlServiceClient() {
        return WebClient.builder()
            .baseUrl(mlServiceUrl)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json")
            // 2MB buffer — ML inference responses can be large when returning
            // full prediction arrays
            .codecs(configurer ->
                configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
    }

    // ── Bean 2: PAHO / CMU Delphi Epidata API ─────────────────────────────────

    // LEARNING NOTE — @Bean naming:
    //   When Spring sees two @Bean methods both returning WebClient, it uses the
    //   method name as the bean name. So:
    //     @Autowired WebClient mlServiceClient;   → injects this bean
    //     @Autowired WebClient pahoApiClient;     → injects this bean
    //   If you use @Qualifier("pahoApiClient") in the injection point, Spring
    //   looks up the bean by that name. This is how multiple beans of the same
    //   type coexist in the application context.
    @Bean
    public WebClient pahoApiClient() {
        return WebClient.builder()
            .baseUrl(pahoApiUrl)
            .defaultHeader("Accept", "application/json")
            // 10MB buffer — PAHO bulk queries (multiple countries, multi-year)
            // can return large JSON payloads; 2MB is not enough
            .codecs(configurer ->
                configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }
}
