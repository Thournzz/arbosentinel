package com.arbosentinel.blue.entity;

// ══════════════════════════════════════════════════════════════════════════════
// BLUE layer — @Entity
// Maps to: paho_caribbean_cases table (V8 migration)
// Source:   PAHO / CMU Delphi Epidata API
//
// LEARNING NOTE — What @Entity does:
//   This annotation tells Hibernate (the JPA implementation Spring Boot uses)
//   that this Java class is a "managed entity" — meaning Hibernate will map it
//   to a database table. Every field annotated with @Column maps to one column.
//   Without @Entity, Hibernate ignores the class entirely.
//
// LEARNING NOTE — Why this class is in the blue/ package:
//   NaviCust colour architecture: blue = data shape (Controllers + Entities).
//   An @Entity class is purely a "what does the data look like" definition.
//   No logic, no calculations, no HTTP — just columns and their Java equivalents.
//   All business logic lives in red/ (@Service). All queries live in purple/ (@Repository).
//
// LEARNING NOTE — Lombok annotations (@Getter @Setter @Builder etc.):
//   Without Lombok, every field needs a written getX() / setX() method — that's
//   hundreds of lines of boilerplate for a class like this. Lombok generates them
//   at compile time via annotation processing:
//     @Getter           → generates getLocationCode(), getDengueCases() etc.
//     @Setter           → generates setLocationCode(), setDengueCases() etc.
//     @NoArgsConstructor → generates PahoCaribCase() — JPA requires a no-arg constructor
//     @AllArgsConstructor → generates PahoCaribCase(id, locationCode, ...) — all fields
//     @Builder          → generates PahoCaribCase.builder().locationCode("jm").build()
//                         The builder pattern is used in the ETL job so we can set
//                         only the fields we have, leaving nullables unset.
// ══════════════════════════════════════════════════════════════════════════════

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paho_caribbean_cases")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PahoCaribCase {

    // LEARNING NOTE — @Id + @GeneratedValue:
    //   @Id marks this field as the PRIMARY KEY column.
    //   @GeneratedValue(IDENTITY) tells Hibernate to let the database assign the ID
    //   automatically using the SERIAL sequence we defined in V8 — we never set id
    //   manually. Hibernate reads it back from the DB after each INSERT.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // LEARNING NOTE — nullable = false in @Column:
    //   This mirrors the NOT NULL constraint in the SQL migration.
    //   Hibernate uses this during schema validation (ddl-auto: validate) — if the
    //   actual column in PostgreSQL is nullable and our entity says nullable=false,
    //   Hibernate throws a startup error. This is how we catch schema drift early.
    @Column(name = "location_code", nullable = false, length = 5)
    private String locationCode;   // e.g. 'jm' = Jamaica, 'tt' = Trinidad and Tobago

    @Column(name = "country_name", nullable = false, length = 80)
    private String countryName;   // Full name: "Jamaica", "Trinidad and Tobago"

    // LEARNING NOTE — Epiweek as Integer:
    //   202301 means year 2023, week 01 (YYYYWW format).
    //   We store it as an integer because:
    //   (a) It sorts correctly: 202252 < 202301
    //   (b) Integer arithmetic extracts year/week: year = epiWeek / 100, week = epiWeek % 100
    //   (c) Smaller storage than VARCHAR, faster index comparisons
    @Column(name = "epi_week", nullable = false)
    private Integer epiWeek;

    // Derived from epiWeek — stored pre-computed so queries can do:
    //   WHERE year = 2023  (fast integer compare)
    //   instead of: WHERE epi_week / 100 = 2023  (function — can't use an index)
    @Column(name = "year", nullable = false)
    private Integer year;

    // Derived from epiWeek: epiWeek % 100. Range 1–53.
    @Column(name = "week_of_year", nullable = false)
    private Integer weekOfYear;

    // LEARNING NOTE — Long for BIGINT:
    //   BIGINT in PostgreSQL = 64-bit integer. Java's int (and Integer) is 32-bit
    //   and maxes out at ~2.1 billion. Some PAHO countries report populations above
    //   that (Brazil > 200M × some scale factor), so we use Long (64-bit, max 9.2×10^18).
    //   Nullable (no nullable=false) — PAHO sometimes omits population data.
    @Column(name = "total_population")
    private Long totalPopulation;

    @Column(name = "dengue_cases")
    private Integer dengueCases;  // Reported cases this epi-week; nullable if not reported

    // LEARNING NOTE — BigDecimal for NUMERIC(10,4):
    //   Never use double or float for epidemiological rates — floating-point binary
    //   representation introduces tiny rounding errors that compound over calculations.
    //   BigDecimal is exact arbitrary-precision decimal arithmetic.
    //   precision=10 = total significant digits, scale=4 = digits after decimal point.
    //   So max value = 999999.9999 — enough for any incidence rate per 100k.
    @Column(name = "incidence_rate", precision = 10, scale = 4)
    private BigDecimal incidenceRate;  // Cases per 100,000 population

    // Circulating DENV serotype (DENV-1 through DENV-4) — almost always null in PAHO data.
    // When not null, it signals elevated risk of severe dengue (antibody-dependent enhancement).
    @Column(name = "serotype", length = 20)
    private String serotype;

    // Data lineage — "PAHO/DELPHI" unless we get data from a different pipeline later.
    // DEFAULT 'PAHO/DELPHI' is set in SQL; here nullable=false enforces it at the Java layer too.
    @Column(name = "data_source", nullable = false, length = 40)
    private String dataSource;

    // LEARNING NOTE — LocalDateTime for TIMESTAMP:
    //   Java's LocalDateTime maps directly to PostgreSQL's TIMESTAMP WITHOUT TIME ZONE.
    //   This records when our ETL job inserted this row — useful for:
    //     (a) Auditing: "when did we last receive data for Jamaica?"
    //     (b) Freshness checks: "is this data more than 30 days old?"
    //   We set this in the ETL job (LocalDateTime.now()) rather than relying on the
    //   SQL DEFAULT NOW() — this way the Java layer is explicit about when it ran.
    @Column(name = "ingested_at", nullable = false)
    private LocalDateTime ingestedAt;
}
