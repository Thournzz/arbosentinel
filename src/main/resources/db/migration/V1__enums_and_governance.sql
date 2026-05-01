-- ============================================================
-- V1__enums_and_governance.sql
-- ArboSentinel — Data governance and type definitions
-- ============================================================

-- Disease types across the entire platform
CREATE TYPE disease_type AS ENUM (
    'dengue', 'malaria', 'zika', 'west_nile', 'chikungunya'
);

-- Risk/severity levels
CREATE TYPE severity_level AS ENUM (
    'low', 'moderate', 'high', 'critical'
);

-- Mr. Prog alert severity
CREATE TYPE alert_status AS ENUM (
    'info', 'moderate', 'high', 'critical'
);

-- Where data came from
CREATE TYPE data_source_type AS ENUM (
    'cdc', 'who', 'drivendata', 'brazil_sinan',
    'kaggle', 'unicef', 'internal'
);

-- ETL run result
CREATE TYPE ingestion_status AS ENUM (
    'success', 'partial', 'failed'
);

-- ============================================================
-- DATA LINEAGE — every imported row traces back here
-- ============================================================

CREATE TABLE data_sources (
    id          SERIAL PRIMARY KEY,
    source_name VARCHAR(100)     NOT NULL,
    source_type data_source_type NOT NULL,
    source_url  TEXT,
    citation    TEXT,
    license     VARCHAR(200),
    version     VARCHAR(50),
    created_at  TIMESTAMP        DEFAULT NOW()
);

-- One row per ETL run
CREATE TABLE ingestion_logs (
    id               SERIAL PRIMARY KEY,
    data_source_id   INTEGER          REFERENCES data_sources(id),
    run_at           TIMESTAMP        DEFAULT NOW(),
    rows_processed   INTEGER,
    rows_inserted    INTEGER,
    rows_skipped     INTEGER,
    rows_failed      INTEGER,
    status           ingestion_status NOT NULL,
    error_message    TEXT
);

-- Flags individual rows with data quality issues
CREATE TABLE data_quality_flags (
    id            SERIAL PRIMARY KEY,
    table_name    VARCHAR(100) NOT NULL,
    row_id        INTEGER      NOT NULL,
    flag_type     VARCHAR(100) NOT NULL,
    field_name    VARCHAR(100),
    flagged_value TEXT,
    flagged_at    TIMESTAMP    DEFAULT NOW(),
    resolved      BOOLEAN      DEFAULT FALSE
);

-- Seed known data sources
INSERT INTO data_sources (source_name, source_type, source_url, citation, license, version) VALUES
('DengAI - DrivenData',        'drivendata', 'https://www.drivendata.org/competitions/44/dengai-predicting-disease-spread/', 'DrivenData. Dengue Forecasting. 2015.', 'Free with attribution', '1.0'),
('CDC West Nile Virus Data',   'cdc',        'https://www.cdc.gov/westnile/data-maps/historic-data.html',                  'CDC. West Nile Virus Historic Data. 2024.', 'Public domain', '2024'),
('WHO Malaria Estimated',      'who',        'https://www.who.int/data/gho/data/themes/malaria',                           'WHO Global Health Observatory. Malaria. 2020.', 'CC BY-NC-SA 3.0', '2020'),
('WHO Malaria Reported',       'who',        'https://www.who.int/data/gho/data/themes/malaria',                           'WHO Global Health Observatory. Malaria Reported. 2020.', 'CC BY-NC-SA 3.0', '2020'),
('CDC Zika 2016',              'kaggle',     'https://www.kaggle.com/datasets/cdc/zika-virus',                             'CDC via Kaggle. Zika Virus 2016 Outbreak Data.', 'Public domain', '1.0'),
('Brazil SINAN Arbovirus',     'brazil_sinan','https://www.kaggle.com/datasets/ramirojose/arboviroses-sinan-sistema-de-informacao-de-agravos','Brazil SINAN Arbovirus Surveillance 2013-2021.', 'Open government data', '1.0'),
('Clinical Vector-borne Dataset', 'kaggle', 'https://www.kaggle.com',                                                     'Tabular clinical dataset for AI-based vector-borne disease classification.', 'Open', '1.0');
