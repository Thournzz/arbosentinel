-- ============================================================
-- V3__epidemiological_data.sql
-- ArboSentinel — All disease case data tables
-- Sources: DengAI, CDC WNV, CDC Zika, WHO Malaria, Brazil SINAN
-- ============================================================

-- DENGUE WEEKLY — DengAI (San Juan + Iquitos, 1990-2008)
-- Features + labels merged into one table
CREATE TABLE dengue_weekly_cases (
    id                               SERIAL PRIMARY KEY,
    city                             VARCHAR(10)  NOT NULL, -- 'sj' = San Juan, 'iq' = Iquitos
    year                             INTEGER      NOT NULL,
    week_of_year                     INTEGER      NOT NULL,
    week_start_date                  DATE,
    total_cases                      INTEGER,
    -- NDVI — vegetation index (mosquito habitat proxy)
    ndvi_ne                          DECIMAL(10,7),
    ndvi_nw                          DECIMAL(10,7),
    ndvi_se                          DECIMAL(10,7),
    ndvi_sw                          DECIMAL(10,7),
    -- Precipitation
    precipitation_amt_mm             DECIMAL(10,4),
    -- Reanalysis climate
    reanalysis_air_temp_k            DECIMAL(12,6),
    reanalysis_avg_temp_k            DECIMAL(12,6),
    reanalysis_dew_point_temp_k      DECIMAL(12,6),
    reanalysis_max_air_temp_k        DECIMAL(12,6),
    reanalysis_min_air_temp_k        DECIMAL(12,6),
    reanalysis_precip_amt_kg_per_m2  DECIMAL(12,6),
    reanalysis_relative_humidity_pct DECIMAL(10,6),
    reanalysis_sat_precip_amt_mm     DECIMAL(12,6),
    reanalysis_specific_humidity_g_kg DECIMAL(12,6),
    reanalysis_tdtr_k                DECIMAL(12,6),
    -- Station readings
    station_avg_temp_c               DECIMAL(10,6),
    station_diur_temp_rng_c          DECIMAL(10,6),
    station_max_temp_c               DECIMAL(10,4),
    station_min_temp_c               DECIMAL(10,4),
    station_precip_mm                DECIMAL(10,4),
    -- Lineage
    data_source_id                   INTEGER REFERENCES data_sources(id),
    ingestion_log_id                 INTEGER REFERENCES ingestion_logs(id),
    UNIQUE (city, year, week_of_year)
);

-- WEST NILE — Annual national totals (1999-2024)
CREATE TABLE west_nile_annual_cases (
    id               SERIAL PRIMARY KEY,
    year             INTEGER UNIQUE NOT NULL,
    reported_cases   INTEGER        NOT NULL,
    data_source_id   INTEGER REFERENCES data_sources(id),
    ingestion_log_id INTEGER REFERENCES ingestion_logs(id)
);

-- WEST NILE — Hospitalizations by case type (2004-2024)
CREATE TABLE west_nile_hospitalizations (
    id                      SERIAL PRIMARY KEY,
    year                    INTEGER UNIQUE NOT NULL,
    neuroinvasive_cases     INTEGER,
    non_neuroinvasive_cases INTEGER,
    data_source_id          INTEGER REFERENCES data_sources(id),
    ingestion_log_id        INTEGER REFERENCES ingestion_logs(id)
);

-- WEST NILE — By US state
CREATE TABLE west_nile_state_cases (
    id               SERIAL PRIMARY KEY,
    case_type        VARCHAR(50) NOT NULL,
    year_range       VARCHAR(20) NOT NULL,
    state_code       CHAR(2)     NOT NULL,
    reported_cases   INTEGER,
    data_source_id   INTEGER REFERENCES data_sources(id),
    ingestion_log_id INTEGER REFERENCES ingestion_logs(id),
    UNIQUE (case_type, year_range, state_code)
);

-- WEST NILE — By month
CREATE TABLE west_nile_monthly_cases (
    id               SERIAL PRIMARY KEY,
    case_type        VARCHAR(50) NOT NULL,
    year_range       VARCHAR(20) NOT NULL,
    month            VARCHAR(10) NOT NULL,
    reported_cases   INTEGER,
    data_source_id   INTEGER REFERENCES data_sources(id),
    ingestion_log_id INTEGER REFERENCES ingestion_logs(id),
    UNIQUE (case_type, year_range, month)
);

-- WEST NILE — Age/sex incidence rates per 100,000
CREATE TABLE west_nile_demographics (
    id               SERIAL PRIMARY KEY,
    age_group        VARCHAR(20) NOT NULL,
    male_rate        DECIMAL(8,4),
    female_rate      DECIMAL(8,4),
    data_source_id   INTEGER REFERENCES data_sources(id),
    ingestion_log_id INTEGER REFERENCES ingestion_logs(id)
);

-- ZIKA — CDC 2016 outbreak
CREATE TABLE zika_cases (
    id               SERIAL PRIMARY KEY,
    report_date      DATE,
    location         VARCHAR(200),
    location_type    VARCHAR(50),
    data_field       VARCHAR(200),
    data_field_code  VARCHAR(50),
    time_period      VARCHAR(100),
    time_period_type VARCHAR(50),
    value            INTEGER,
    unit             VARCHAR(50),
    data_source_id   INTEGER REFERENCES data_sources(id),
    ingestion_log_id INTEGER REFERENCES ingestion_logs(id)
);

-- MALARIA — WHO estimated (with confidence intervals)
CREATE TABLE malaria_estimated_cases (
    id               SERIAL PRIMARY KEY,
    country          VARCHAR(200) NOT NULL,
    year             INTEGER      NOT NULL,
    cases_median     BIGINT,
    cases_min        BIGINT,
    cases_max        BIGINT,
    deaths_median    INTEGER,
    deaths_min       INTEGER,
    deaths_max       INTEGER,
    who_region       VARCHAR(100),
    data_source_id   INTEGER REFERENCES data_sources(id),
    ingestion_log_id INTEGER REFERENCES ingestion_logs(id),
    UNIQUE (country, year)
);

-- MALARIA — WHO reported
CREATE TABLE malaria_reported_cases (
    id               SERIAL PRIMARY KEY,
    country          VARCHAR(200)  NOT NULL,
    year             INTEGER       NOT NULL,
    reported_cases   DECIMAL(15,1),
    reported_deaths  DECIMAL(15,1),
    who_region       VARCHAR(100),
    data_source_id   INTEGER REFERENCES data_sources(id),
    ingestion_log_id INTEGER REFERENCES ingestion_logs(id),
    UNIQUE (country, year)
);

-- BRAZIL SINAN — patient-level arbovirus surveillance
-- Covers: dengue (A90), zika, chikungunya (2013-2021)
CREATE TABLE brazil_sinan_cases (
    id                   SERIAL PRIMARY KEY,
    disease_type         disease_type NOT NULL,
    classification       INTEGER,       -- 1=dengue, 2=dengue+warning signs, 3=severe dengue
    municipality_code    BIGINT,
    sex                  CHAR(1),       -- M / F
    notification_date    DATE,
    symptom_onset_date   DATE,
    outcome              INTEGER,       -- 1=cure, 2=death, 3=ongoing, 4=unknown
    disease_code         VARCHAR(10),   -- ICD-10: A90=dengue
    state_code           VARCHAR(2),
    municipality_name    VARCHAR(200),
    state_name           VARCHAR(100),
    year                 INTEGER,
    age_days             INTEGER,       -- SINAN encodes age in days
    notification_week    INTEGER,
    symptom_onset_week   INTEGER,
    data_source_id       INTEGER REFERENCES data_sources(id),
    ingestion_log_id     INTEGER REFERENCES ingestion_logs(id)
);
