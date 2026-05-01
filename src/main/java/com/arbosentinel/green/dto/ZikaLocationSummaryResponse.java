package com.arbosentinel.green.dto;

// ================================================
// GREEN layer — Zika location summary DTO
// Projected from JPQL aggregate: location, SUM(value)
// Powers: Zika outbreak heatmap
// ================================================

public record ZikaLocationSummaryResponse(
        String location,
        Long totalCases
) {}
