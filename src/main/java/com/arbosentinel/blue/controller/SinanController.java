package com.arbosentinel.blue.controller;

import com.arbosentinel.green.dto.*;
import com.arbosentinel.red.SinanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sinan")
@RequiredArgsConstructor
public class SinanController {

    private final SinanService sinanService;

    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<List<SinanCountResponse>>> getAnnualCountsByDisease() {
        return ResponseEntity.ok(ApiResponse.ok(sinanService.getAnnualCountsByDisease()));
    }

    @GetMapping("/states/{diseaseType}/{year}")
    public ResponseEntity<ApiResponse<List<Object[]>>> getStateCountsByDiseaseAndYear(
            @PathVariable String diseaseType,
            @PathVariable Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(
            sinanService.getStateCountsByDiseaseAndYear(diseaseType, year)));
    }

    @GetMapping("/outcomes/{diseaseType}")
    public ResponseEntity<ApiResponse<List<Object[]>>> getOutcomeDistribution(
            @PathVariable String diseaseType) {
        return ResponseEntity.ok(ApiResponse.ok(sinanService.getOutcomeDistribution(diseaseType)));
    }

    @GetMapping("/sex/{diseaseType}")
    public ResponseEntity<ApiResponse<List<Object[]>>> getSexBreakdown(
            @PathVariable String diseaseType) {
        return ResponseEntity.ok(ApiResponse.ok(sinanService.getSexBreakdown(diseaseType)));
    }

    @GetMapping("/weekly/{diseaseType}/{year}")
    public ResponseEntity<ApiResponse<List<Object[]>>> getWeeklyTimeline(
            @PathVariable String diseaseType,
            @PathVariable Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(sinanService.getWeeklyTimeline(diseaseType, year)));
    }
}
