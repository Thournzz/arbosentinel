package com.arbosentinel.blue.controller;

import com.arbosentinel.green.dto.*;
import com.arbosentinel.red.RiskScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskScoreService riskScoreService;

    // All active outbreak risk scores — powers OutbreakRadar component
    @GetMapping
    public ResponseEntity<ApiResponse<List<OutbreakRiskResponse>>> getAllActiveRiskScores() {
        return ResponseEntity.ok(ApiResponse.ok(riskScoreService.getAllActiveRiskScores()));
    }

    // Risk score for a specific disease
    @GetMapping("/{diseaseType}")
    public ResponseEntity<ApiResponse<OutbreakRiskResponse>> getRiskForDisease(
            @PathVariable String diseaseType) {
        return ResponseEntity.ok(ApiResponse.ok(
            riskScoreService.getCurrentRiskForDisease(diseaseType)));
    }
}
