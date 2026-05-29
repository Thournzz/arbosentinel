package com.arbosentinel.green.dto;

// ══════════════════════════════════════════════════════════════════════════════
// GREEN layer — PAHO Caribbean case response DTO
// Returned by PahoController to the frontend.
//
// LEARNING NOTE — Why a DTO (Data Transfer Object) instead of returning the entity?
//   The @Entity class (PahoCaribCase) is Hibernate's internal representation —
//   it's coupled to the database schema. If we return it directly from the API:
//     (a) We expose internal fields the frontend doesn't need (ingestedAt, dataSource)
//     (b) Lazy-loaded Hibernate proxy fields can throw exceptions during JSON serialisation
//     (c) Any DB schema change immediately breaks the API contract
//   A DTO is a clean boundary: the entity is our private DB concern,
//   the DTO is our public API contract. We decide exactly what the frontend receives.
//
// LEARNING NOTE — Java records (Java 16+):
//   A record is a special class designed for immutable data containers.
//   public record Foo(String x, Integer y) {} auto-generates:
//     - A constructor: new Foo("hello", 42)
//     - Getters: foo.x(), foo.y()   (note: no "get" prefix)
//     - equals(), hashCode(), toString() — all based on the fields
//   Records are immutable — no setters. Perfect for DTOs where we build once, send once.
//   Jackson serialises records to JSON automatically:
//     { "locationCode": "jm", "countryName": "Jamaica", ... }
//
// LEARNING NOTE — Boxed types (Integer, Long, BigDecimal vs int, long, double):
//   All nullable fields use boxed types (Integer) not primitives (int).
//   A primitive int cannot be null — it defaults to 0 if not set.
//   An Integer can be null, which serialises to JSON null, communicating "data not available"
//   rather than falsely sending 0 cases when PAHO didn't report that week.
// ══════════════════════════════════════════════════════════════════════════════

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PahoCaribCaseResponse(
        Integer         id,
        String          locationCode,       // e.g. "jm"
        String          countryName,        // e.g. "Jamaica"
        Integer         epiWeek,            // YYYYWW: 202301
        Integer         year,               // 2023
        Integer         weekOfYear,         // 1–53
        Long            totalPopulation,    // Registered population (nullable)
        Integer         dengueCases,        // Reported dengue cases (nullable)
        BigDecimal      incidenceRate,      // Cases per 100,000 (nullable)
        String          serotype,           // DENV-1 through DENV-4 (almost always null)
        String          dataSource,         // "PAHO/DELPHI"
        LocalDateTime   ingestedAt          // When ETL loaded this row
) {}
