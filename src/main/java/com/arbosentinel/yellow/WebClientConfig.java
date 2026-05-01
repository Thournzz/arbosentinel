package com.arbosentinel.yellow;

// ================================================
// YELLOW layer — WebClient configuration
// Configures reactive HTTP client for Python FastAPI
// ML microservice (scikit-learn dengue model)
// mlServiceUrl set in application.properties:
//   ml.service.url=http://localhost:8000
// ================================================

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${arbosentinel.ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    @Bean
    public WebClient mlServiceClient() {
        return WebClient.builder()
            .baseUrl(mlServiceUrl)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json")
            // 30s timeout — ML inference can take a moment on cold start
            .codecs(configurer ->
                configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB
            .build();
    }
}
