-- ============================================================
-- V5__application_tables_views_indexes.sql
-- ArboSentinel — ML, Risk Scores, Alerts + Indexes + Views
-- ============================================================

-- ML PREDICTIONS — stored results from Python FastAPI
CREATE TABLE ml_predictions (
    id                 SERIAL PRIMARY KEY,
    disease_type       disease_type NOT NULL,
    region_name        VARCHAR(200),
    prediction_date    TIMESTAMP    DEFAULT NOW(),
    week_of_year       INTEGER,
    avg_temp_c         DECIMAL(8,4),
    precipitation_mm   DECIMAL(10,4),
    humidity_percent   DECIMAL(8,4),
    ndvi               DECIMAL(10,7),
    predicted_cases    INTEGER,
    risk_score         DECIMAL(5,2),    -- 0 to 100
    confidence_percent DECIMAL(5,2),
    model_version      VARCHAR(50),
    input_payload      JSONB            -- full input snapshot for audit trail
);

-- OUTBREAK RISK SCORES — computed by RED service layer on schedule
CREATE TABLE outbreak_risk_scores (
    id                   SERIAL PRIMARY KEY,
    disease_type         disease_type   NOT NULL,
    region_name          VARCHAR(200),
    computed_at          TIMESTAMP      DEFAULT NOW(),
    risk_score           DECIMAL(5,2),  -- 0 to 100
    risk_level           severity_level,
    contributing_factors JSONB,         -- {"trend_pct":40,"ndvi":0.82,"cases_ytd":2847}
    expires_at           TIMESTAMP
);

-- MR. PROG ALERTS — proactive monitoring messages
-- Populated by ORANGE @Scheduled layer, served by BLUE controller
CREATE TABLE mr_prog_alerts (
    id            SERIAL PRIMARY KEY,
    disease_type  disease_type,
    alert_status  alert_status NOT NULL,
    alert_message TEXT         NOT NULL,
    region_name   VARCHAR(200),
    triggered_at  TIMESTAMP    DEFAULT NOW(),
    is_active     BOOLEAN      DEFAULT TRUE,
    expires_at    TIMESTAMP
);

-- ============================================================
-- INDEXES — query performance
-- ============================================================

-- Dengue
CREATE INDEX idx_dengue_city_year   ON dengue_weekly_cases(city, year);
CREATE INDEX idx_dengue_year_week   ON dengue_weekly_cases(year, week_of_year);
CREATE INDEX idx_dengue_cases       ON dengue_weekly_cases(total_cases);
CREATE INDEX idx_dengue_city_cases  ON dengue_weekly_cases(city, total_cases DESC);

-- West Nile
CREATE INDEX idx_wnv_year           ON west_nile_annual_cases(year);
CREATE INDEX idx_wnv_state          ON west_nile_state_cases(state_code);
CREATE INDEX idx_wnv_month          ON west_nile_monthly_cases(month);
CREATE INDEX idx_wnv_hosp_year      ON west_nile_hospitalizations(year);

-- Malaria
CREATE INDEX idx_malaria_est_cy     ON malaria_estimated_cases(country, year);
CREATE INDEX idx_malaria_rep_cy     ON malaria_reported_cases(country, year);
CREATE INDEX idx_malaria_who_region ON malaria_estimated_cases(who_region, year);
CREATE INDEX idx_malaria_deaths     ON malaria_estimated_cases(deaths_median DESC);

-- Zika
CREATE INDEX idx_zika_location      ON zika_cases(location);
CREATE INDEX idx_zika_date          ON zika_cases(report_date);
CREATE INDEX idx_zika_field         ON zika_cases(data_field);

-- Brazil SINAN
CREATE INDEX idx_sinan_disease_yr   ON brazil_sinan_cases(disease_type, year);
CREATE INDEX idx_sinan_state        ON brazil_sinan_cases(state_code);
CREATE INDEX idx_sinan_onset        ON brazil_sinan_cases(symptom_onset_date);
CREATE INDEX idx_sinan_municipality ON brazil_sinan_cases(municipality_code);

-- Application tables
CREATE INDEX idx_risk_disease_time  ON outbreak_risk_scores(disease_type, computed_at DESC);
CREATE INDEX idx_risk_active        ON outbreak_risk_scores(risk_level, computed_at DESC);
CREATE INDEX idx_alerts_active      ON mr_prog_alerts(is_active, triggered_at DESC);
CREATE INDEX idx_alerts_disease     ON mr_prog_alerts(disease_type, is_active);
CREATE INDEX idx_predict_disease    ON ml_predictions(disease_type, prediction_date DESC);

-- Pharmacology
CREATE INDEX idx_drug_class         ON pharmacology_drugs(drug_class);
CREATE INDEX idx_drug_who           ON pharmacology_drugs(who_essential_medicine);
CREATE INDEX idx_indication_disease ON drug_disease_indications(disease_id, indication_type);

-- Symptom profiles
CREATE INDEX idx_symptoms_disease   ON clinical_symptom_profiles(disease_id, phase);

-- Data quality
CREATE INDEX idx_quality_table      ON data_quality_flags(table_name, resolved);
CREATE INDEX idx_ingestion_source   ON ingestion_logs(data_source_id, run_at DESC);

-- ============================================================
-- MATERIALIZED VIEWS
-- ============================================================

-- Global disease totals — powers Surveillance Dashboard hero numbers
CREATE MATERIALIZED VIEW mv_disease_totals AS
SELECT 'dengue'       AS disease,
       SUM(total_cases)            AS total_cases,
       MAX(year)                   AS latest_year
FROM dengue_weekly_cases
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
WHERE data_field ILIKE '%confirmed%'
UNION ALL
SELECT 'chikungunya',
       COUNT(*),
       MAX(year)
FROM brazil_sinan_cases
WHERE disease_type = 'chikungunya';

-- 4-week rolling average for dengue — powers trend sparklines
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
    ) AS rolling_4wk_avg
FROM dengue_weekly_cases
WHERE total_cases IS NOT NULL;

-- West Nile year-over-year trend
CREATE MATERIALIZED VIEW mv_west_nile_trend AS
SELECT
    year,
    reported_cases,
    LAG(reported_cases) OVER (ORDER BY year)                                          AS prev_year_cases,
    reported_cases - LAG(reported_cases) OVER (ORDER BY year)                         AS yoy_change,
    ROUND(
        (reported_cases - LAG(reported_cases) OVER (ORDER BY year))::DECIMAL
        / NULLIF(LAG(reported_cases) OVER (ORDER BY year), 0) * 100, 2
    )                                                                                  AS yoy_change_pct
FROM west_nile_annual_cases
ORDER BY year;

-- Malaria burden by WHO region
CREATE MATERIALIZED VIEW mv_malaria_by_region AS
SELECT
    who_region,
    year,
    SUM(cases_median)  AS total_cases_median,
    SUM(deaths_median) AS total_deaths_median
FROM malaria_estimated_cases
WHERE who_region IS NOT NULL
GROUP BY who_region, year
ORDER BY who_region, year;

-- Dengue peak week by city — for seasonal prediction context
CREATE MATERIALIZED VIEW mv_dengue_seasonal_peaks AS
SELECT
    city,
    week_of_year,
    ROUND(AVG(total_cases), 2) AS avg_cases_this_week,
    MAX(total_cases)           AS max_cases_ever_this_week,
    COUNT(*)                   AS years_of_data
FROM dengue_weekly_cases
WHERE total_cases IS NOT NULL
GROUP BY city, week_of_year
ORDER BY city, avg_cases_this_week DESC;
