package com.arbosentinel.green.dto;

public record WestNileStateResponse(
        Integer id,
        String caseType,
        String yearRange,
        String stateCode,
        Integer reportedCases
) {}
