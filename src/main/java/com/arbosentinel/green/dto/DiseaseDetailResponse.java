package com.arbosentinel.green.dto;

// ================================================
// GREEN layer — Full disease detail DTO
// Aggregates: disease + vectors + symptoms + drugs
// Powers: Pathogen library detail page
// ================================================

import java.util.List;

public record DiseaseDetailResponse(
        Integer id,
        String diseaseType,
        String commonName,
        String pathogenFamily,
        String pathogenSpecies,
        String genomeType,
        String structure,
        String transmissionRoute,
        Integer firstIdentifiedYear,
        String firstIdentifiedLocation,
        String whoClassification,
        Integer incubationMinDays,
        Integer incubationMaxDays,
        String acutePhaseDescription,
        String complications,
        String recoveryDescription,
        String treatmentSummary,
        List<VectorResponse> vectors,
        List<ClinicalSymptomResponse> symptoms,
        List<PharmacologyDrugResponse> drugs
) {}
