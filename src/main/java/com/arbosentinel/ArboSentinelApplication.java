package com.arbosentinel;

// ================================================
// BLUE layer — Entry point
// Rule: ONLY Blue lives in main()
// All other colors are called THROUGH Blue (Spring context)
// ================================================

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching       // PINK — activates @Cacheable across service layer
@EnableScheduling    // ORANGE — activates @Scheduled ETL jobs
@EnableAsync         // ORANGE — activates @Async for non-blocking ingestion
public class ArboSentinelApplication {

    public static void main(String[] args) {
        // BLUE Command Line — only entry point
        // Spring context boots all other colors from here
        SpringApplication.run(ArboSentinelApplication.class, args);
    }
}
