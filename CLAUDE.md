# ArboSentinel — Project Context

## What this is
Arboviral pathogen intelligence platform. Tracks Dengue, Malaria, Zika, West Nile, Chikungunya.
Built for the UWI Mosquito Control and Research Unit (Director: Dr. Simone Laura Sandiford).
Status: backend complete, frontend complete, negotiating commission.

## Stack
- Backend: Java Spring Boot 3.2.3 on port 9191
- Database: PostgreSQL — database `arbosentinel`, user `arbo_user`
- ML: Python FastAPI on port 8000 (arbosentinel-ml/)
- Frontend: React + Vite + TypeScript on port 5175 (arbosentinel-frontend/)

## NaviCust colour architecture
blue/ = @RestController + @Entity
purple/ = @Repository
green/ = DTOs + MapStruct mappers
yellow/ = @Configuration (Security, Cache, WebClient, CORS)
white/ = JWT util + GlobalExceptionHandler
orange/ = @Scheduled ETL jobs
pink/ = Caffeine cache config
red/ = @Service (all business logic)

## Key decisions already made
- PostgreSQL custom enums: disease_type, severity_level, alert_status, ingestion_status, data_source_type
- JSONB via @JdbcTypeCode(SqlTypes.JSON) — no extra library
- JWT: JJWT 0.12.3 — uses Keys.hmacShaKeyFor(secret.getBytes(UTF_8)), NOT Base64 decode
- Composite PK on disease_vectors: @EmbeddedId with DiseaseVectorId implements Serializable
- ArboDataSource (not DataSource) — avoids javax.sql.DataSource collision
- WestNileStateResponse DTO exists to fix List<Object[]> return type issue
- Spring Security 6: SecurityFilterChain bean, no WebSecurityConfigurerAdapter
- pgAdmin runs on 8080 so Spring Boot uses 9191

## Flyway migrations
V1 = enums + core tables
V2 = disease + vector tables
V3 = disease_vectors composite PK table
V4 = ML + risk scoring tables
V5 = pharmacology tables
V6 = biopesticide + source credibility fields

## ETL data files (C:/Users/jourd/Downloads/)
dengue_features_train.csv + dengue_labels_train.csv — DengAI
WNV_Annual.csv, WNV_StateCase.csv, WNV_Hospitalization.csv, WNV_Monthly.csv, WNV_Demographics.csv — CDC
zika.csv — CDC 2016
all_arb_cid.csv — Brazil SINAN (large file, batch=500)
malaria_reported_cases.csv — WHO

## Frontend location
C:/database and programming git good aka masters course/group assignment/CYCF_Project/arbosentinel-frontend/
Running: npm run dev (port 5175)

## ML service location
C:/database and programming git good aka masters course/group assignment/CYCF_Project/arbosentinel-ml/
Running: uvicorn main:app --port 8000

## Current state
Backend: BUILD SUCCESS — all layers complete
Frontend: live on port 5175 — 5 pages, mock data
ML: FastAPI ready, rule-based fallback active (no trained model yet)
About page: professional framing — "DEVELOPED FOR · MCRU"
Commission status: pending negotiation with Dr. Sandiford
