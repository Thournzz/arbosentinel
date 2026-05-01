package com.arbosentinel.green.dto;

import java.util.List;

// ================================================
// GREEN layer — Dashboard hero statistics DTO
// Aggregates across all diseases for landing page
// Powers: Hero stat cards at top of Surveillance page
// Sourced from mv_disease_totals materialized view
// ================================================

public record DashboardStatsResponse(
        List<DiseaseTotalResponse> diseaseTotals,
        Long totalUnresolvedFlags,
        Long totalFailedIngestions
) {
    // Inner record for each disease row in mv_disease_totals
    public record DiseaseTotalResponse(
            String disease,
            Long totalCases,
            Integer latestYear
    ) {}
}
