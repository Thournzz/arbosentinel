package com.arbosentinel.green.dto;

// ================================================
// GREEN layer — Disease summary DTO
// Used in lists (pathogen library grid, disease dropdowns)
// Full clinical detail → DiseaseDetailResponse
// ================================================

public record DiseaseResponse(
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
        String treatmentSummary
) {}
