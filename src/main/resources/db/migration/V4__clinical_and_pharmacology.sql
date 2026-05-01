-- ============================================================
-- V4__clinical_and_pharmacology.sql
-- ArboSentinel — Pharmacology reference + symptom profiles
-- Powers: Pharmacology page, Pathogen Library clinical sections
-- Dedicated to Dr. Simone Sandiford
-- ============================================================

-- PHARMACOLOGY DRUGS
CREATE TABLE pharmacology_drugs (
    id                     SERIAL PRIMARY KEY,
    drug_name              VARCHAR(200) NOT NULL,
    drug_class             VARCHAR(100),
    mechanism_of_action    TEXT,
    dosing_adult           TEXT,
    dosing_pediatric       TEXT,
    key_interactions       TEXT,
    contraindications      TEXT,
    who_essential_medicine BOOLEAN  DEFAULT FALSE,
    interaction_warning    BOOLEAN  DEFAULT FALSE,
    created_at             TIMESTAMP DEFAULT NOW()
);

-- DRUG <-> DISEASE indications
CREATE TABLE drug_disease_indications (
    id              SERIAL PRIMARY KEY,
    drug_id         INTEGER REFERENCES pharmacology_drugs(id) ON DELETE CASCADE,
    disease_id      INTEGER REFERENCES diseases(id)           ON DELETE CASCADE,
    indication_type VARCHAR(50),   -- 'treatment','prophylaxis','supportive'
    evidence_level  VARCHAR(100),  -- 'who_recommended','first_line','second_line','adjunct'
    notes           TEXT,
    UNIQUE (drug_id, disease_id, indication_type)
);

-- CLINICAL SYMPTOM PROFILES — derived from French clinical dataset
CREATE TABLE clinical_symptom_profiles (
    id                    SERIAL PRIMARY KEY,
    disease_id            INTEGER REFERENCES diseases(id),
    symptom_name_en       VARCHAR(200) NOT NULL,
    symptom_name_fr       VARCHAR(200),
    phase                 VARCHAR(50),  -- 'acute','complication','recovery'
    prevalence_percent    DECIMAL(5,2),
    is_pathognomonic      BOOLEAN DEFAULT FALSE,
    clinical_significance VARCHAR(200)
);

-- ============================================================
-- SEED — Pharmacology drugs
-- ============================================================

-- ANTIMALARIALS
INSERT INTO pharmacology_drugs (drug_name, drug_class, mechanism_of_action, dosing_adult, dosing_pediatric, key_interactions, contraindications, who_essential_medicine, interaction_warning) VALUES

('Artemether-Lumefantrine', 'ACT (Artemisinin-based Combination Therapy)',
 'Artemether: rapid parasite clearance via free radical generation damaging parasite proteins and membranes. Lumefantrine: interferes with heme detoxification in the parasite digestive vacuole.',
 '80/480mg twice daily for 3 days, with fatty food (improves absorption)',
 'Weight-based dosing: 5-14kg = 1 tablet BD, 15-24kg = 2 tablets BD, 25-34kg = 3 tablets BD',
 'CYP3A4 substrate — avoid strong inhibitors (ketoconazole, ritonavir) and inducers (rifampicin). QT prolongation risk — avoid with other QT-prolonging drugs.',
 'First trimester pregnancy (use quinine + clindamycin instead). Known hypersensitivity.',
 TRUE, TRUE),

('Artesunate', 'Artemisinin',
 'Endoperoxide bridge activation generates free radicals, damaging parasite proteins and membranes. Faster action than artemether.',
 'IV/IM: 2.4 mg/kg at 0, 12, 24 hours, then daily. Oral: 4mg/kg/day for 3 days (in combination).',
 'IV: 2.4 mg/kg at 0, 12, 24h then daily. Rectal artesunate: pre-referral single dose.',
 'Few significant drug interactions. Post-treatment haemolysis monitoring required for IV use.',
 'Hypersensitivity. Oral not recommended as monotherapy.',
 TRUE, FALSE),

('Atovaquone-Proguanil', 'Antimalarial combination',
 'Atovaquone: inhibits mitochondrial electron transport (cytochrome bc1 complex). Proguanil: inhibits dihydrofolate reductase disrupting folate synthesis.',
 'Prophylaxis: 250/100mg daily, starting 1-2 days before travel, continue 7 days after. Treatment: 4 tablets daily for 3 days.',
 'Weight-based prophylaxis starting at 11kg.',
 'Reduced absorption with metoclopramide. Rifampicin reduces plasma levels significantly. Warfarin — monitor INR.',
 'Severe renal impairment (CrCl <30 mL/min). Pregnancy — safety not established.',
 TRUE, TRUE),

('Chloroquine', 'Aminoquinoline',
 'Accumulates in parasite digestive vacuole, inhibits heme polymerization causing toxic heme buildup.',
 'Treatment: 10mg/kg loading, then 5mg/kg at 6, 24, 48 hours. Prophylaxis: 300mg weekly.',
 'Treatment: 10mg base/kg. Prophylaxis: 5mg/kg weekly.',
 'Antacids reduce absorption. Ciclosporin levels increased. QT prolongation with amiodarone.',
 'Retinal disease. G6PD deficiency (hemolytic risk). Chloroquine-resistant P. falciparum (most of world).',
 TRUE, TRUE),

('Primaquine', 'Aminoquinoline (8-aminoquinoline)',
 'Mechanism not fully elucidated. Active against liver stages (hypnozoites) of P. vivax and P. ovale. Gametocidal against P. falciparum.',
 'Radical cure P. vivax/ovale: 15mg/day for 14 days (after chloroquine). G6PD-deficient: 45mg weekly for 8 weeks.',
 '0.25-0.5 mg/kg daily for 14 days.',
 'Avoid with other hemolytic drugs. MAOIs — avoid concurrent use.',
 'G6PD deficiency — causes severe hemolytic anaemia. Pregnancy. Breastfeeding if infant G6PD unknown.',
 TRUE, TRUE),

-- DENGUE / SUPPORTIVE CARE
('Paracetamol (Acetaminophen)', 'Analgesic / Antipyretic',
 'Inhibits prostaglandin synthesis in the CNS. Exact mechanism incompletely understood.',
 '500-1000mg every 4-6 hours. Maximum 4g/day.',
 '15mg/kg every 4-6 hours. Maximum 4 doses/day.',
 'Warfarin — prolonged use increases anticoagulant effect. Alcohol — hepatotoxicity risk increased.',
 'Severe hepatic impairment. Alcohol use disorder.',
 TRUE, FALSE),

('Ibuprofen (NSAID)', 'NSAID — COX inhibitor',
 'Non-selective COX-1 and COX-2 inhibitor reducing prostaglandin synthesis.',
 '200-400mg every 4-6 hours. Maximum 1200mg/day OTC.',
 '5-10mg/kg every 6-8 hours.',
 'Warfarin, aspirin, other NSAIDs. ACE inhibitors — reduced antihypertensive effect. Lithium toxicity.',
 'CONTRAINDICATED IN DENGUE — increases bleeding risk due to thrombocytopenia. Peptic ulcer. Renal impairment. Pregnancy (third trimester).',
 FALSE, TRUE),

-- VECTOR CONTROL
('DEET (N,N-Diethyl-meta-toluamide)', 'Insect repellent',
 'Repels insects by blocking olfactory receptors that detect human skin odor compounds.',
 'Apply to exposed skin. 20-30% concentration for tropical endemic areas. Reapply per product instructions.',
 'Safe over 2 months. Avoid eyes and mouth. 10-30% concentration.',
 'Sunscreen — reduces DEET efficacy when co-applied. Apply repellent over sunscreen.',
 'Avoid on infants under 2 months. Avoid ingestion.',
 FALSE, FALSE),

('Permethrin', 'Pyrethroid insecticide',
 'Disrupts sodium channel function in insect neurons causing paralysis and death.',
 'Topical clothing treatment: spray garments and allow to dry. Not for skin application.',
 'Clothing/net treatment only. Not applied to skin.',
 'Highly toxic to cats — keep treated items away from cats until fully dry.',
 'Direct skin application. Aquatic organisms — highly toxic.',
 FALSE, FALSE);

-- ============================================================
-- SEED — Drug-disease indications
-- ============================================================

-- Malaria treatments (disease_id = 2)
INSERT INTO drug_disease_indications (drug_id, disease_id, indication_type, evidence_level, notes) VALUES
(1, 2, 'treatment', 'who_recommended', 'First-line for uncomplicated P. falciparum malaria globally'),
(2, 2, 'treatment', 'who_recommended', 'First-line for severe malaria — IV route. Superior to IV quinine.'),
(3, 2, 'prophylaxis', 'first_line', 'Preferred for short-trip travellers to high-risk areas'),
(3, 2, 'treatment', 'first_line', 'Alternative for uncomplicated malaria in areas with ACT resistance'),
(4, 2, 'treatment', 'second_line', 'First-line only where P. falciparum is still chloroquine-sensitive (limited areas)'),
(4, 2, 'prophylaxis', 'second_line', 'Weekly prophylaxis where chloroquine sensitivity confirmed'),
(5, 2, 'treatment', 'who_recommended', 'Radical cure for P. vivax and P. ovale to eliminate liver hypnozoites');

-- Dengue supportive care (disease_id = 1)
INSERT INTO drug_disease_indications (drug_id, disease_id, indication_type, evidence_level, notes) VALUES
(6, 1, 'supportive', 'who_recommended', 'ONLY safe analgesic/antipyretic in dengue — NSAIDs strictly contraindicated'),
(7, 1, 'supportive', 'adjunct', 'CONTRAINDICATED — listed here as contraindication reference only. Never use in dengue.');

-- Zika supportive care (disease_id = 3)
INSERT INTO drug_disease_indications (drug_id, disease_id, indication_type, evidence_level, notes) VALUES
(6, 3, 'supportive', 'first_line', 'Paracetamol for fever and pain. NSAIDs avoided until dengue co-infection ruled out.');

-- Chikungunya (disease_id = 5)
INSERT INTO drug_disease_indications (drug_id, disease_id, indication_type, evidence_level, notes) VALUES
(7, 5, 'supportive', 'first_line', 'NSAIDs appropriate for joint pain management in chikungunya (unlike dengue)'),
(6, 5, 'supportive', 'first_line', 'Paracetamol for fever management');

-- Vector control — applies to all mosquito-borne diseases
INSERT INTO drug_disease_indications (drug_id, disease_id, indication_type, evidence_level, notes) VALUES
(8, 1, 'prophylaxis', 'who_recommended', 'Personal protection — repels Aedes aegypti'),
(8, 2, 'prophylaxis', 'who_recommended', 'Personal protection — repels Anopheles mosquitoes'),
(8, 3, 'prophylaxis', 'who_recommended', 'Personal protection during Zika epidemic areas'),
(8, 4, 'prophylaxis', 'who_recommended', 'Personal protection — repels Culex mosquitoes'),
(8, 5, 'prophylaxis', 'who_recommended', 'Personal protection — repels Aedes vectors'),
(9, 1, 'prophylaxis', 'who_recommended', 'Clothing/net treatment for Aedes protection'),
(9, 2, 'prophylaxis', 'who_recommended', 'Insecticide-treated bed nets (ITNs) — core malaria prevention tool');

-- ============================================================
-- SEED — Clinical symptom profiles (key symptoms per disease)
-- ============================================================

INSERT INTO clinical_symptom_profiles (disease_id, symptom_name_en, symptom_name_fr, phase, prevalence_percent, is_pathognomonic, clinical_significance) VALUES
-- Dengue (1)
(1, 'Retro-orbital pain',    'Douleur rétro-orbitrale',    'acute', 70.0, TRUE,  'Hallmark diagnostic feature — pain behind the eyes'),
(1, 'High fever',            'Haute température',           'acute', 98.0, FALSE, 'Sudden onset, >38.5°C'),
(1, 'Severe headache',       'Céphalée sévère',             'acute', 90.0, FALSE, 'Frontal predominance'),
(1, 'Myalgia/Arthralgia',    'Douleur musculaire/articulaire', 'acute', 85.0, FALSE, 'Breakbone fever — severe bone pain'),
(1, 'Rash',                  'Éruption cutanée',            'acute', 50.0, FALSE, 'Maculopapular, appears day 3-5'),
(1, 'Thrombocytopenia',      'Thrombocytopénie',            'complication', 60.0, FALSE, 'Platelet count <100,000 — monitor closely'),
(1, 'Bleeding',              'Saignement',                  'complication', 15.0, FALSE, 'Gum bleeding, epistaxis, petechiae — warning sign'),

-- Malaria (2)
(2, 'Cyclical fever',        'Fièvre cyclique',             'acute', 95.0, TRUE,  'Tertian (P. vivax/ovale) or quartan (P. malariae) periodicity'),
(2, 'Chills/Rigors',         'Frissons',                    'acute', 90.0, FALSE, 'Preceding fever spike — classic pattern'),
(2, 'Headache',              'Maux de tête',                'acute', 88.0, FALSE, 'Diffuse, often severe'),
(2, 'Anaemia',               'Anémie/Pâleur',               'complication', 70.0, FALSE, 'Haemolysis of parasitised red cells'),
(2, 'Splenomegaly',          'Splénomégalie',               'complication', 50.0, FALSE, 'Enlarged spleen — chronic/repeated infection'),
(2, 'Jaundice',              'Ictère',                      'complication', 30.0, FALSE, 'Haemolytic jaundice — severe malaria indicator'),

-- Zika (3)
(3, 'Conjunctivitis',        'Inflammation de conjonctivite', 'acute', 55.0, TRUE, 'Non-purulent conjunctivitis — distinguishes from dengue'),
(3, 'Low-grade fever',       'Fièvre légère',               'acute', 65.0, FALSE, 'Less severe than dengue or chikungunya'),
(3, 'Maculopapular rash',    'Éruption morbilliforme',      'acute', 90.0, FALSE, 'Prominent rash — often first presenting symptom'),
(3, 'Joint pain',            'Douleur articulaire',         'acute', 65.0, FALSE, 'Less severe than chikungunya'),

-- West Nile (4)
(4, 'Fever',                 'Fièvre',                      'acute', 80.0, FALSE, 'Present in symptomatic cases (20% of infections)'),
(4, 'Headache',              'Maux de tête',                'acute', 75.0, FALSE, 'Prominent feature'),
(4, 'Encephalitis',          'Encéphalite',                 'complication', 1.0,  TRUE,  'Neuroinvasive — confusion, altered consciousness, paralysis'),
(4, 'Flaccid paralysis',     'Paralysie flasque',           'complication', 0.5,  FALSE, 'Polio-like acute flaccid paralysis — rare but severe'),

-- Chikungunya (5)
(5, 'Severe joint pain',     'Douleur articulaire sévère',  'acute', 99.0, TRUE,  'Pathognomonic — debilitating polyarthritis, bilateral and symmetric'),
(5, 'Joint swelling',        'Gonflement des articulations','acute', 70.0, FALSE, 'Periarticular swelling'),
(5, 'Sudden-onset fever',    'Fièvre brutale',              'acute', 99.0, FALSE, '>39°C, sudden onset'),
(5, 'Chronic arthritis',     'Arthrite chronique',          'complication', 30.0, FALSE, 'Persists months to years after acute infection');
