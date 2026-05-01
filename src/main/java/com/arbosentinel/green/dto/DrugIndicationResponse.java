package com.arbosentinel.green.dto;

public record DrugIndicationResponse(
        Integer id,
        Integer diseaseId,
        String indicationType,    // "treatment" | "prophylaxis" | "supportive"
        String evidenceLevel,     // "who_recommended" | "first_line" | "second_line" | "adjunct"
        String notes
) {}
