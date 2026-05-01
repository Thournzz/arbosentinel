package com.arbosentinel.green.dto;

// ================================================
// GREEN layer — WHO malaria estimated burden DTO
// Includes confidence intervals (min/max)
// Powers: Malaria page country detail cards + maps
// ================================================

public record MalariaEstimatedResponse(
        Integer id,
        String country,
        Integer year,
        Long casesMedian,
        Long casesMin,
        Long casesMax,
        Integer deathsMedian,
        Integer deathsMin,
        Integer deathsMax,
        String whoRegion
) {}
