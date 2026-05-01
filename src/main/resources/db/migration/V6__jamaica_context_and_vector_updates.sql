-- ============================================================
-- V6__jamaica_context_and_vector_updates.sql
-- ArboSentinel — Jamaica focus + additional vector species
-- Context: Dr. Simone Sandiford, Director, UWI Mosquito Control
-- and Research Unit, Faculty of Medical Sciences, UWI Mona
-- ============================================================

-- Add Aedes vittatus — subject of Dr. Sandiford's 2025 paper
-- Invasive species recently identified in Jamaica
INSERT INTO vectors (genus, species, common_name, geographic_range, breeding_conditions, activity_peak) VALUES
('Aedes', 'vittatus',     'Rock pool mosquito',
 'Africa, Asia, Middle East — invasive in Jamaica (2025)',
 'Rock pools, treeholes, small natural containers — tolerates desiccation',
 'Daytime'),

('Aedes', 'aegypti formosus', 'Forest yellow fever mosquito',
 'Sub-Saharan Africa',
 'Tree holes, natural containers in forested areas',
 'Daytime'),

('Anopheles', 'albimanus', 'White-footed mosquito',
 'Central America, Caribbean including Jamaica',
 'Grassy edges of streams, marshes, rice paddies, slow-moving water',
 'Night — primary malaria vector in Caribbean region'),

('Culex', 'quinquefasciatus', 'Southern house mosquito',
 'Tropical and subtropical worldwide including Jamaica',
 'Stagnant polluted water, drains, septic tanks, catch basins',
 'Night');

-- Jamaica region entry
INSERT INTO regions (name, country_code, region_type, lat, lng, who_region) VALUES
('Jamaica',  'JAM', 'country', 18.1096, -77.2975, 'AMRO'),
('Kingston', 'JAM', 'city',    17.9970, -76.7936,  'AMRO'),
('Mona',     'JAM', 'city',    18.0024, -76.7478,  'AMRO');

-- Add biopesticide vector control agents to pharmacology
-- Relevant to Dr. Sandiford's biopesticide research
INSERT INTO pharmacology_drugs (drug_name, drug_class, mechanism_of_action, dosing_adult, dosing_pediatric, key_interactions, contraindications, who_essential_medicine, interaction_warning) VALUES

('Bacillus thuringiensis israelensis (Bti)', 'Biological larvicide (biopesticide)',
 'Produces crystalline endotoxins (Cry and Cyt proteins) that bind to midgut epithelial receptors of mosquito larvae causing cell lysis and larval death. Highly species-specific — does not harm non-target organisms.',
 'Applied to larval breeding sites: 1-5 kg/ha granules or 0.5-2 L/ha liquid formulation depending on water depth and organic content.',
 'Same application — targets larvae, not humans. No direct human dosing.',
 'No significant interactions with other public health interventions. Compatible with chemical larvicides.',
 'No contraindications for human or environmental safety. WHO approved for use in drinking water sources.',
 FALSE, FALSE),

('Spinosad', 'Biological larvicide (spinosyn)',
 'Derived from Saccharopolyspora spinosa. Acts on nicotinic acetylcholine receptors and GABA receptors causing hyperexcitation of the insect nervous system and death.',
 'Larval control: 0.1-0.5 ppm in water. Extended release granules for container habitats.',
 'Larval habitat treatment only.',
 'Low toxicity to mammals. Moderately toxic to aquatic invertebrates at high concentrations.',
 'Avoid direct application to aquatic environments with non-target crustaceans.',
 FALSE, FALSE),

('Temephos (Abate)', 'Organophosphate larvicide',
 'Inhibits acetylcholinesterase in mosquito larvae causing neuromuscular dysfunction and death.',
 'Sand granule formulation: 1 mg/L (1 ppm) in water containers.',
 'Same — applied to water containers, not directly to humans.',
 'Cholinesterase inhibitor — avoid combined exposure with other organophosphates or carbamates.',
 'Use being phased out in some countries due to environmental persistence concerns. Not for use in potable water (unlike Bti).',
 FALSE, TRUE),

('Lambda-cyhalothrin', 'Pyrethroid insecticide (indoor residual spraying)',
 'Type II pyrethroid. Prolongs opening of voltage-gated sodium channels in insect neurons causing repetitive firing, paralysis and death.',
 'Indoor residual spraying (IRS): 20-30 mg/m² on indoor walls. Space spraying during outbreaks.',
 'Environmental application only.',
 'Highly toxic to fish and aquatic invertebrates — avoid runoff to water bodies. Toxic to bees.',
 'Areas with confirmed pyrethroid-resistant Aedes populations (increasing globally). Avoid near water bodies.',
 FALSE, TRUE);

-- Link biopesticide drugs to all mosquito-borne diseases
-- drug_ids: Bti=10, Spinosad=11, Temephos=12, Lambda-cyhalothrin=13
-- (these are sequential after the 9 drugs seeded in V4)
INSERT INTO drug_disease_indications (drug_id, disease_id, indication_type, evidence_level, notes)
SELECT d.id, dis.id, 'prophylaxis', 'who_recommended',
       'Larval source reduction — core vector control intervention'
FROM pharmacology_drugs d, diseases dis
WHERE d.drug_name IN ('Bacillus thuringiensis israelensis (Bti)', 'Spinosad', 'Temephos (Abate)')
AND dis.disease_type IN ('dengue', 'malaria', 'zika', 'west_nile', 'chikungunya');

INSERT INTO drug_disease_indications (drug_id, disease_id, indication_type, evidence_level, notes)
SELECT d.id, dis.id, 'prophylaxis', 'who_recommended',
       'Indoor residual spraying and space spraying during outbreaks'
FROM pharmacology_drugs d, diseases dis
WHERE d.drug_name = 'Lambda-cyhalothrin'
AND dis.disease_type IN ('dengue', 'malaria', 'zika', 'chikungunya');

-- Add Aedes vittatus vector links for dengue and chikungunya
-- (potential competence — basis of 2025 Jamaica study)
INSERT INTO disease_vectors (disease_id, vector_id, is_primary, notes)
SELECT dis.id, v.id, FALSE,
       'Invasive in Jamaica — vector competence under investigation (Sandiford et al. 2025)'
FROM diseases dis, vectors v
WHERE v.species = 'vittatus'
AND dis.disease_type IN ('dengue', 'chikungunya');
