package com.arbosentinel.green.dto;

// ================================================
// GREEN layer — Global malaria burden per year
// Projected from JPQL GROUP BY aggregate query
// Powers: Dashboard hero stat + timeline chart
// ================================================

public record MalariaBurdenResponse(
        Integer year,
        Long totalCasesMedian,
        Long totalDeathsMedian
) {}
