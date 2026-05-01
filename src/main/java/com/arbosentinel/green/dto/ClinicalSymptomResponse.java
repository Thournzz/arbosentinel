package com.arbosentinel.green.dto;

import java.math.BigDecimal;

// ================================================
// GREEN layer — Clinical symptom DTO
// Bilingual (EN + FR) — dataset origin is French
// Powers: Pathogen library symptom cards
// ================================================

public record ClinicalSymptomResponse(
        Integer id,
        Integer diseaseId,
        String symptomNameEn,
        String symptomNameFr,
        String phase,              // "acute" | "complication" | "recovery"
        BigDecimal prevalencePercent,
        Boolean isPathognomonic,
        String clinicalSignificance
) {}
