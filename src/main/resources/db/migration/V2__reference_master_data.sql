-- ============================================================
-- V2__reference_master_data.sql
-- ArboSentinel — Disease profiles, vectors, regions
-- Powers: Pathogen Library page, Map page, About page
-- ============================================================

-- DISEASE PROFILES
CREATE TABLE diseases (
    id                        SERIAL PRIMARY KEY,
    disease_type              disease_type UNIQUE NOT NULL,
    common_name               VARCHAR(100),
    pathogen_family           VARCHAR(100),
    pathogen_species          VARCHAR(200),
    genome_type               VARCHAR(100),
    structure                 VARCHAR(100),
    transmission_route        VARCHAR(200),
    first_identified_year     INTEGER,
    first_identified_location VARCHAR(200),
    who_classification        VARCHAR(200),
    incubation_min_days       INTEGER,
    incubation_max_days       INTEGER,
    acute_phase_description   TEXT,
    complications             TEXT,
    recovery_description      TEXT,
    treatment_summary         TEXT
);

-- VECTOR PROFILES
CREATE TABLE vectors (
    id                  SERIAL PRIMARY KEY,
    genus               VARCHAR(100) NOT NULL,
    species             VARCHAR(100) NOT NULL,
    common_name         VARCHAR(100),
    geographic_range    TEXT,
    breeding_conditions TEXT,
    activity_peak       VARCHAR(200)
);

-- DISEASE <-> VECTOR (many-to-many)
CREATE TABLE disease_vectors (
    disease_id INTEGER REFERENCES diseases(id) ON DELETE CASCADE,
    vector_id  INTEGER REFERENCES vectors(id)  ON DELETE CASCADE,
    is_primary BOOLEAN DEFAULT FALSE,
    notes      TEXT,
    PRIMARY KEY (disease_id, vector_id)
);

-- GEOGRAPHIC REGIONS
CREATE TABLE regions (
    id               SERIAL PRIMARY KEY,
    name             VARCHAR(200) NOT NULL,
    country_code     CHAR(3),
    state_code       CHAR(5),
    region_type      VARCHAR(50)  NOT NULL,
    parent_region_id INTEGER      REFERENCES regions(id),
    lat              DECIMAL(10,6),
    lng              DECIMAL(11,6),
    who_region       VARCHAR(100)
);

-- ============================================================
-- SEED — Disease profiles
-- ============================================================
INSERT INTO diseases (disease_type, common_name, pathogen_family, pathogen_species, genome_type, structure, transmission_route, first_identified_year, first_identified_location, who_classification, incubation_min_days, incubation_max_days, acute_phase_description, complications, recovery_description, treatment_summary) VALUES

('dengue', 'Dengue Fever', 'Flaviviridae', 'Dengue virus (DENV 1-4)', 'Positive-sense ssRNA', 'Enveloped, icosahedral', 'Mosquito bite (Aedes spp.)', 1779, 'Asia', 'Arboviral disease — notifiable', 4, 10,
 'High fever, severe headache, retro-orbital pain, myalgia, arthralgia, rash',
 'Dengue hemorrhagic fever (DHF), dengue shock syndrome (DSS), organ impairment',
 '2-7 days for uncomplicated cases',
 'Supportive care, fluid management, paracetamol for fever. Avoid NSAIDs — increases bleeding risk.'),

('malaria', 'Malaria', 'Plasmodiidae', 'Plasmodium falciparum, P. vivax, P. ovale, P. malariae, P. knowlesi', 'DNA (nuclear and mitochondrial)', 'Intracellular parasite', 'Mosquito bite (Anopheles spp.)', 1880, 'Africa/Asia', 'Notifiable — WHO Global Malaria Programme', 7, 30,
 'Cyclical fever, chills, headache, muscle pain, fatigue, nausea, vomiting',
 'Severe malaria: cerebral malaria, acute respiratory distress, organ failure, severe anaemia',
 'Uncomplicated: 3-7 days with treatment. Severe: weeks with complications',
 'First-line: Artemisinin-based Combination Therapies (ACTs). Severe: IV Artesunate.'),

('zika', 'Zika Fever', 'Flaviviridae', 'Zika virus (ZIKV)', 'Positive-sense ssRNA', 'Enveloped, icosahedral', 'Mosquito bite (Aedes spp.), sexual transmission, vertical transmission', 1947, 'Uganda (Zika Forest)', 'Arboviral disease — PHEIC declared 2016', 3, 14,
 'Mild fever, rash, conjunctivitis, muscle and joint pain, malaise, headache. 80% asymptomatic.',
 'Microcephaly (congenital), Guillain-Barre syndrome, neurological complications',
 '2-7 days. Usually mild.',
 'No specific antiviral. Supportive care. Paracetamol. Avoid NSAIDs until dengue ruled out.'),

('west_nile', 'West Nile Fever', 'Flaviviridae', 'West Nile virus (WNV)', 'Positive-sense ssRNA', 'Enveloped, icosahedral', 'Mosquito bite (Culex spp.), blood transfusion, organ transplant, vertical', 1937, 'Uganda (West Nile district)', 'Arboviral disease — nationally notifiable (USA)', 2, 14,
 'Fever, headache, body aches, joint pain, vomiting, diarrhea, rash. 80% asymptomatic.',
 'West Nile neuroinvasive disease: meningitis, encephalitis, acute flaccid paralysis',
 '3-6 days mild. Neuroinvasive: weeks to months, permanent neurological deficits possible',
 'No specific antiviral or vaccine for humans. Supportive care. IV fluids, respiratory support for neuroinvasive cases.'),

('chikungunya', 'Chikungunya', 'Togaviridae', 'Chikungunya virus (CHIKV)', 'Positive-sense ssRNA', 'Enveloped, icosahedral', 'Mosquito bite (Aedes aegypti, Aedes albopictus)', 1952, 'Tanzania (Makonde Plateau)', 'Arboviral disease — re-emerging globally', 1, 12,
 'Sudden onset fever, severe joint pain (arthralgia), myalgia, headache, nausea, fatigue, rash',
 'Chronic arthritis (months to years), rare neurological complications, rare fatalities in elderly',
 'Acute: 7-10 days. Joint pain may persist months.',
 'No specific antiviral. NSAIDs or corticosteroids for joint pain. Rest and hydration.');

-- ============================================================
-- SEED — Vectors
-- ============================================================
INSERT INTO vectors (genus, species, common_name, geographic_range, breeding_conditions, activity_peak) VALUES
('Aedes',     'aegypti',      'Yellow fever mosquito',        'Tropical and subtropical worldwide',                   'Standing water in artificial containers, tires, flower pots, uncovered tanks', 'Daytime — peak early morning and late afternoon'),
('Aedes',     'albopictus',   'Asian tiger mosquito',         'Tropical, subtropical, and temperate regions globally', 'Small water containers, leaf axils, tree holes, artificial containers',          'Daytime'),
('Anopheles', 'gambiae',      'African malaria mosquito',     'Sub-Saharan Africa',                                   'Clear, slow-moving or stagnant fresh water, rice paddies, puddles',             'Night — peak midnight to early morning'),
('Anopheles', 'stephensi',    'Asian malaria mosquito',       'South Asia, Middle East, East Africa (emerging)',       'Stored water containers, cisterns, wells',                                       'Night'),
('Culex',     'pipiens',      'Common house mosquito',        'Worldwide temperate and subtropical',                   'Stagnant water, drains, catch basins, polluted water',                          'Dusk and night');

-- ============================================================
-- SEED — Disease-Vector links
-- ============================================================
-- Dengue
INSERT INTO disease_vectors (disease_id, vector_id, is_primary) VALUES
(1, 1, true),   -- Dengue + Aedes aegypti (primary)
(1, 2, false);  -- Dengue + Aedes albopictus (secondary)

-- Malaria
INSERT INTO disease_vectors (disease_id, vector_id, is_primary) VALUES
(2, 3, true),   -- Malaria + Anopheles gambiae (primary)
(2, 4, false);  -- Malaria + Anopheles stephensi

-- Zika
INSERT INTO disease_vectors (disease_id, vector_id, is_primary) VALUES
(3, 1, true),   -- Zika + Aedes aegypti (primary)
(3, 2, false);  -- Zika + Aedes albopictus

-- West Nile
INSERT INTO disease_vectors (disease_id, vector_id, is_primary) VALUES
(4, 5, true);   -- West Nile + Culex pipiens (primary)

-- Chikungunya
INSERT INTO disease_vectors (disease_id, vector_id, is_primary) VALUES
(5, 1, true),   -- Chikungunya + Aedes aegypti (primary)
(5, 2, true);   -- Chikungunya + Aedes albopictus (co-primary)
