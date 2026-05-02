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

## Deployed URLs (Railway — project: luminous-flow)
- API:      https://arbosentinel-api-production.up.railway.app
- ML:       https://arbosentinel-ml-production.up.railway.app
- Frontend: https://arbosentinel.vercel.app
- DB:       postgres.railway.internal:5432 (private, railway managed)

## GitHub repos
- Backend:  https://github.com/Thournzz/arbosentinel
- Frontend: https://github.com/Thournzz/arbosentinel-frontend
- ML:       local only (no GitHub repo yet)

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
V7 = fix CHAR→VARCHAR (bpchar vs varchar Hibernate validate mismatch)

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

## Current state (as of May 2026)
Backend:  DEPLOYED on Railway — Spring Boot live, Flyway V1-V7 applied
Database: DEPLOYED on Railway — PostgreSQL online, seeded reference data
ML:       DEPLOYED on Railway — FastAPI live, rule-based fallback active (no DengAI training yet)
Frontend: DEPLOYED on Vercel — arbosentinel.vercel.app
          SurveillancePage, PathogenLibraryPage, PharmacologyPage → live Railway API
          DenguePage → mock data (ETL not yet run)
          Risk gauges → mock data (ML model not yet trained)

## Railway env vars (arbosentinel-api service)
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=f203be538c4fc7b881fdadfdd7fbc45b85af42539f17b8ab61bf79ebf7172055e22a88b8ab461d5033366912225c7a10
DATABASE_URL=jdbc:postgresql://postgres.railway.internal:5432/railway
PGUSER=postgres
PGPASSWORD=YIPEIhjKXyoXDOJgQrbZAesBZQNIZcpL
CORS_ORIGINS=https://arbosentinel.vercel.app
ML_SERVICE_URL=https://arbosentinel-ml-production.up.railway.app

## Next steps
1. Send email to Dr. Sandiford (commission negotiation)
2. Run ETL jobs — load DengAI, CDC, SINAN CSV files via POST /api/etl/* (needs JWT)
3. Train ML model — POST /train on ML service with DengAI CSV paths
4. LinkedIn post (after email)
5. PharmaSentinel — compensation conversation with Dr. Gossell-Williams

## About page
Professional framing — "DEVELOPED FOR · MCRU · Director: Dr. Simone Laura Sandiford"
No tribute/gift language. Commission status: pending negotiation.
