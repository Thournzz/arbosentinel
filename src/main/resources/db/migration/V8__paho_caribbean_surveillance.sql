-- ══════════════════════════════════════════════════════════════════════════════
-- V8 — PAHO Caribbean Surveillance Table
-- Source: PAHO / CMU Delphi Epidata API
-- https://api.delphi.cmu.edu/epidata/paho_dengue/
--
-- LEARNING NOTE — Why a separate table instead of reusing dengue_weekly_cases?
-- DengAI data (dengue_weekly_cases) has ~24 climate variables per row — it was
-- designed for ML training. PAHO data has a completely different shape: it is
-- pure surveillance output (cases + incidence rate + population) with no climate
-- variables. Merging them into one table would force most columns to be NULL
-- for one source or the other — bad schema design. Separate tables = clear data
-- lineage and clean schema.
--
-- LEARNING NOTE — UNIQUE constraint for idempotency:
-- The UNIQUE(location_code, epi_week) constraint means the database itself
-- enforces that the same country + week combination can only appear once.
-- If the ETL job runs twice (restart, redeploy), the second attempt gets a
-- constraint violation on insert and we handle it with ON CONFLICT DO NOTHING.
-- This is safer than checking in application code — the DB is always the
-- authoritative source of truth.
--
-- LEARNING NOTE — Epiweek format (YYYYWW):
-- PAHO and CDC use "epidemiological week" numbers, not calendar dates.
-- Epiweek 202301 means: year 2023, week 1. Week 1 always starts on a Sunday
-- and contains January 4th (US CDC definition) or the first Monday of the year
-- (ISO 8601 definition). PAHO uses the CDC/MMWR definition.
-- Stored as INTEGER (e.g., 202301) — year = epiweek/100, week = epiweek%100.
-- ══════════════════════════════════════════════════════════════════════════════

CREATE TABLE paho_caribbean_cases (
    id               SERIAL       PRIMARY KEY,

    -- WHO location code — ISO 3166-1 alpha-2 but PAHO uses their own short codes
    -- e.g., 'jm' = Jamaica, 'tt' = Trinidad and Tobago, 'bb' = Barbados
    location_code    VARCHAR(5)   NOT NULL,

    -- Human-readable country name — derived from location_code in the ETL mapper
    country_name     VARCHAR(80)  NOT NULL,

    -- YYYYWW epidemiological week — e.g., 202301 = 2023 week 1
    epi_week         INTEGER      NOT NULL,

    -- Derived from epi_week — stored separately so SQL queries filter by year easily
    -- without doing integer division every time: WHERE year = 2023
    year             INTEGER      NOT NULL,

    -- Derived from epi_week: epi_week % 100 gives the week number (01–53)
    week_of_year     INTEGER      NOT NULL,

    -- Total registered population for this country in the reporting period
    -- Used to calculate incidence rate — BIGINT because some countries exceed 2^31
    total_population BIGINT,

    -- Reported dengue cases for this country-week
    dengue_cases     INTEGER,

    -- Cases per 100,000 population — the standard epidemiological rate denominator
    -- NUMERIC(10,4) gives us 6 digits before decimal, 4 after — enough precision
    incidence_rate   NUMERIC(10, 4),

    -- Circulating serotype (DENV-1 through DENV-4) — usually NULL in PAHO data
    -- Multiple active serotypes increase risk of severe dengue (antibody-dependent
    -- enhancement — prior DENV-1 infection makes DENV-2 infection more dangerous)
    serotype         VARCHAR(20),

    -- Data lineage — which source and API version provided this row
    data_source      VARCHAR(40)  NOT NULL DEFAULT 'PAHO/DELPHI',

    -- Timestamp the ETL job loaded this record — for audit and freshness checks
    ingested_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ── Idempotency constraint ─────────────────────────────────────────────────────
-- Prevents the same country + week being inserted twice.
-- ETL job uses ON CONFLICT DO NOTHING to handle restarts gracefully.
ALTER TABLE paho_caribbean_cases
    ADD CONSTRAINT uq_paho_location_epiweek UNIQUE (location_code, epi_week);

-- ── Indexes for common query patterns ─────────────────────────────────────────
-- LEARNING NOTE — Why these specific indexes?
-- idx_paho_location: most queries filter by country — "show me Jamaica's data"
-- idx_paho_year:     year-range queries for trend charts
-- idx_paho_loc_year: composite for "Jamaica 2020-2024" — covers both filters at once
--                    with a single index scan instead of two separate ones

CREATE INDEX idx_paho_location  ON paho_caribbean_cases (location_code);
CREATE INDEX idx_paho_year      ON paho_caribbean_cases (year);
CREATE INDEX idx_paho_loc_year  ON paho_caribbean_cases (location_code, year);
