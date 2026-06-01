-- ══════════════════════════════════════════════════════════════════════════════
-- V9 — Caribbean Scope Refactor: Remove Brazil SINAN, filter DengAI to San Juan
-- Reason: Dr. Sandiford directive — Caribbean regional focus only.
--         Brazil SINAN is Brazil-national data, not Caribbean surveillance.
--         DengAI Iquitos is Peru (South America), not Caribbean.
--         San Juan, Puerto Rico (DengAI city code 'sj') IS Caribbean — kept.
--
-- Changes made:
--   1. Drop brazil_sinan_cases indexes
--   2. Drop brazil_sinan_cases table
--   3. Rebuild mv_disease_totals — removes SINAN UNION branch
--   4. Rebuild mv_dengue_rolling_avg — filtered to 'sj' (San Juan) only
-- ══════════════════════════════════════════════════════════════════════════════

-- ── Step 1: Drop SINAN indexes ────────────────────────────────────────────────
-- Must drop indexes before dropping the table they belong to.
-- IF EXISTS = safe to re-run (idempotent) — won't error if already gone.
DROP INDEX IF EXISTS idx_sinan_disease_yr;
DROP INDEX IF EXISTS idx_sinan_state;
DROP INDEX IF EXISTS idx_sinan_onset;
DROP INDEX IF EXISTS idx_sinan_municipality;

-- ── Step 2: Drop the materialized views that reference brazil_sinan_cases ─────
-- PostgreSQL cannot DROP a table while a view still depends on it.
-- We drop both views now and recreate them below without the SINAN reference.
DROP MATERIALIZED VIEW IF EXISTS mv_disease_totals;
DROP MATERIALIZED VIEW IF EXISTS mv_dengue_rolling_avg;

-- ── Step 3: Drop the brazil_sinan_cases table ──────────────────────────────
-- CASCADE drops any remaining dependent objects (foreign keys, etc.)
-- No data loss risk — SINAN ETL was never run on this database.
DROP TABLE IF EXISTS brazil_sinan_cases CASCADE;

-- ── Step 4: Rebuild mv_disease_totals — Caribbean + regional sources only ────
-- Removed: brazil_sinan_cases UNION branch (Brazil, not Caribbean)
-- Kept:    dengue_weekly_cases (San Juan, Puerto Rico — Caribbean)
--          west_nile_annual_cases (regional context/educational)
--          malaria_estimated_cases (WHO — Hispaniola focus in Caribbean)
--          zika_cases (Caribbean 2015-2016 outbreak)
-- Added:   paho_caribbean_cases (PAHO — primary Caribbean surveillance source)
CREATE MATERIALIZED VIEW mv_disease_totals AS
SELECT 'dengue'     AS disease,
       SUM(total_cases) AS total_cases,
       MAX(year)        AS latest_year
FROM dengue_weekly_cases
UNION ALL
SELECT 'dengue_caribbean',
       COALESCE(SUM(dengue_cases), 0),
       MAX(year)
FROM paho_caribbean_cases
UNION ALL
SELECT 'west_nile',
       SUM(reported_cases),
       MAX(year)
FROM west_nile_annual_cases
UNION ALL
SELECT 'malaria',
       SUM(cases_median),
       MAX(year)
FROM malaria_estimated_cases
UNION ALL
SELECT 'zika',
       SUM(value),
       MAX(EXTRACT(YEAR FROM report_date)::INTEGER)
FROM zika_cases
WHERE data_field ILIKE '%confirmed%';

-- ── Step 5: Rebuild mv_dengue_rolling_avg — San Juan (Caribbean) only ────────
-- Removed: Iquitos, Peru ('iq') — South America, not Caribbean
-- Kept:    San Juan, Puerto Rico ('sj') — Caribbean island
CREATE MATERIALIZED VIEW mv_dengue_rolling_avg AS
SELECT
    city,
    year,
    week_of_year,
    total_cases,
    ROUND(
        AVG(total_cases) OVER (
            PARTITION BY city
            ORDER BY year, week_of_year
            ROWS BETWEEN 3 PRECEDING AND CURRENT ROW
        ), 2
    ) AS rolling_avg_4wk
FROM dengue_weekly_cases
WHERE city = 'sj';  -- San Juan, Puerto Rico only
