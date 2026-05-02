-- ══════════════════════════════════════════════════════════════════════════════
-- V7 — Fix CHAR → VARCHAR for Hibernate schema validation
-- PostgreSQL stores CHAR(n) as bpchar internally.
-- Hibernate's String mapping expects varchar — this mismatch fails ddl-auto=validate.
-- All four affected columns hold short codes; semantics are unchanged.
-- ══════════════════════════════════════════════════════════════════════════════

-- regions (V2)
ALTER TABLE regions ALTER COLUMN country_code TYPE VARCHAR(3);
ALTER TABLE regions ALTER COLUMN state_code   TYPE VARCHAR(5);

-- west_nile_state_cases (V3)
ALTER TABLE west_nile_state_cases ALTER COLUMN state_code TYPE VARCHAR(2);

-- brazil_sinan_cases (V3)
ALTER TABLE brazil_sinan_cases ALTER COLUMN sex TYPE VARCHAR(1);
