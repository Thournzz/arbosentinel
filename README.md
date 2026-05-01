# ArboSentinel — Arboviral Pathogen Intelligence Platform

Mosquito-borne disease surveillance and ML forecasting platform tracking Dengue, Malaria, Zika, West Nile Virus, and Chikungunya.

Built with Java Spring Boot 3 · Python FastAPI · React · PostgreSQL.

---

## Project Structure

```
arbosentinel/                        ← Spring Boot backend (this repo)
  src/main/java/com/arbosentinel/
    blue/        @RestController + @Entity
    purple/      @Repository (JPA)
    green/       DTOs + MapStruct mappers
    yellow/      @Configuration (Security, Cache, WebClient, CORS)
    white/       JWT util, global exception handler
    orange/      @Scheduled ETL jobs (CSV ingestion)
    pink/        @Cacheable strategies (Caffeine)
    red/         @Service (business logic, risk scoring, ML calls)

arbosentinel-frontend/               ← React + Vite frontend (see separate README)
arbosentinel-ml/                     ← Python FastAPI ML microservice (see separate README)
```

---

## Tech Stack

| Layer       | Technology |
|-------------|-----------|
| Backend     | Java 21 · Spring Boot 3.2.3 · Spring Security 6 |
| ORM         | JPA/Hibernate 6 · Flyway migrations |
| Database    | PostgreSQL 17.5 · JSONB · Custom enum types |
| Cache       | Caffeine · 14 named caches |
| ML          | Python FastAPI · scikit-learn GradientBoostingRegressor |
| Frontend    | React 18 · Vite · TypeScript |
| Auth        | JWT (JJWT 0.12.3) · HS256 · Stateless sessions |

---

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 17.5 running on port 5432
- Python 3.10+ (for ML microservice)
- Node.js 18+ (for frontend)
- pgAdmin or psql to create the database

---

## Database Setup

```sql
-- Run in psql or pgAdmin
CREATE DATABASE arbosentinel;
CREATE USER arbo_user WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE arbosentinel TO arbo_user;
```

Flyway runs migrations automatically on startup (V1 through V6).
**Never change `ddl-auto` to `create` or `update` in production — it is set to `validate`.**

---

## Configuration

Copy and edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/arbosentinel
spring.datasource.username=arbo_user
spring.datasource.password=YOUR_DB_PASSWORD

# JWT — change this before any deployment
arbosentinel.jwt.secret=replace-with-a-long-random-string-minimum-32-chars
arbosentinel.jwt.expiration=86400000

# Data files — absolute path to folder containing all CSV files
arbosentinel.data.dir=C:/Users/jourd/Downloads

# ML microservice URL
arbosentinel.ml.service.url=http://localhost:8000

# Server port (pgAdmin uses 8080, so we use 9191)
server.port=9191
```

---

## Data Files Required

Download and place in `arbosentinel.data.dir`:

| File | Source | Disease |
|------|--------|---------|
| `dengue_features_train.csv` | DrivenData — DengAI | Dengue |
| `dengue_labels_train.csv` | DrivenData — DengAI | Dengue |
| `WNV_Annual.csv` | CDC West Nile Virus Data | West Nile |
| `WNV_StateCase.csv` | CDC West Nile Virus Data | West Nile |
| `WNV_Hospitalization.csv` | CDC West Nile Virus Data | West Nile |
| `WNV_Monthly.csv` | CDC West Nile Virus Data | West Nile |
| `WNV_Demographics.csv` | CDC West Nile Virus Data | West Nile |
| `zika.csv` | Kaggle — CDC 2016 Zika Outbreak | Zika |
| `all_arb_cid.csv` | Kaggle — Brazil SINAN | Dengue/Zika/Chikungunya |
| `malaria_reported_cases.csv` | WHO Malaria Report | Malaria |

ETL jobs run once on startup and skip if already loaded (idempotent). Check `ingestion_log` table for status.

---

## Running the Backend

```bash
# From project root
mvn spring-boot:run

# Or build first
mvn clean package -DskipTests
java -jar target/arbosentinel-*.jar
```

Backend starts on `http://localhost:9191`

---

## API Overview

| Endpoint | Auth | Description |
|----------|------|-------------|
| `POST /api/auth/register` | Public | Register user |
| `POST /api/auth/login` | Public | Get JWT token |
| `GET /api/dashboard/stats` | Public | Hero stats |
| `GET /api/diseases` | Public | All disease profiles |
| `GET /api/dengue/weekly` | Public | Weekly case data |
| `GET /api/malaria/burden` | Public | Malaria burden data |
| `GET /api/westnile/annual` | Public | WNV annual trend |
| `GET /api/zika/locations` | Public | Zika case locations |
| `GET /api/ml/predictions/{disease}` | Public | Latest ML predictions |
| `GET /api/ml/predictions/high-risk` | Public | High-risk predictions |
| `POST /api/ml/run/dengue` | **Auth required** | Trigger ML prediction |
| `GET /api/alerts` | Public | Active MrProg alerts |
| `GET /api/risk-scores` | Public | Current risk scores |
| `GET /api/pharmacology/drugs` | Public | Drug profiles |

---

## NaviCust Colour Architecture

Each package has one role. Never mix responsibilities across colours.

| Colour | Package | Spring Annotation | Role |
|--------|---------|-------------------|------|
| 🔵 Blue | `blue/` | `@RestController` + `@Entity` | HTTP layer + data model |
| 🟣 Purple | `purple/` | `@Repository` | Database queries only |
| 🟢 Green | `green/` | POJO | DTOs and MapStruct mappers |
| 🟡 Yellow | `yellow/` | `@Configuration` | Security, cache, CORS, WebClient |
| ⚪ White | `white/` | `@Component` | JWT, exception handler |
| 🟠 Orange | `orange/` | `@Scheduled` | ETL jobs, data ingestion |
| 🩷 Pink | `pink/` | `@Configuration` | Caffeine cache config |
| 🔴 Red | `red/` | `@Service` | All business logic |
