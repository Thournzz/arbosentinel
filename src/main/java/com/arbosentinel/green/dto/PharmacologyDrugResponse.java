package com.arbosentinel.green.dto;

import java.util.List;

// ================================================
// GREEN layer — Pharmacology drug DTO
// Includes indication list when returned in disease context
// Powers: Pharmacology page drug cards
// ================================================

public record PharmacologyDrugResponse(
        Integer id,
        String drugName,
        String drugClass,
        String mechanismOfAction,
        String dosingAdult,
        String dosingPediatric,
        String keyInteractions,
        String contraindications,
        Boolean whoEssentialMedicine,
        Boolean interactionWarning,
        List<DrugIndicationResponse> indications   // null when not in disease context
) {}
