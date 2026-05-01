package com.arbosentinel.blue.controller;

import com.arbosentinel.green.dto.ApiResponse;
import com.arbosentinel.green.dto.MlPredictionResponse;
import com.arbosentinel.red.MlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
public class MlController {

    private final MlService mlService;

    // Predict dengue cases for a city + week with climate inputs
    // Protected — requires authentication to trigger ML runs
    @PostMapping("/run/dengue")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MlPredictionResponse>> predictDengue(
            @RequestParam String city,
            @RequestParam Integer weekOfYear,
            @RequestParam Double avgTempC,
            @RequestParam Double precipMm,
            @RequestParam Double humidityPct,
            @RequestParam Double ndviNe) {
        return ResponseEntity.ok(ApiResponse.ok(
            mlService.predictDengueCases(city, weekOfYear, avgTempC, precipMm, humidityPct, ndviNe)
        ));
    }

    // Latest predictions — public read
    @GetMapping("/predictions/{diseaseType}")
    public ResponseEntity<ApiResponse<List<MlPredictionResponse>>> getLatestPredictions(
            @PathVariable String diseaseType,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(mlService.getLatestPredictions(diseaseType, limit)));
    }

    // Predictions above risk threshold
    @GetMapping("/predictions/high-risk")
    public ResponseEntity<ApiResponse<List<MlPredictionResponse>>> getHighRiskPredictions(
            @RequestParam(defaultValue = "70.0") double threshold) {
        return ResponseEntity.ok(ApiResponse.ok(mlService.getHighRiskPredictions(threshold)));
    }
}
