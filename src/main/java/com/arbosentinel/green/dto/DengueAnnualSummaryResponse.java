package com.arbosentinel.green.dto;

// ================================================
// GREEN layer — Dengue annual summary DTO
// Projected from JPQL aggregate query in PURPLE layer
// Powers: Year-over-year bar chart on Surveillance page
// ================================================

public record DengueAnnualSummaryResponse(
        String city,
        Integer year,
        Long totalCases
) {}
