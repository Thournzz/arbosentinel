package com.arbosentinel.blue.controller;

import com.arbosentinel.green.dto.*;
import com.arbosentinel.red.WestNileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/westnile")
@RequiredArgsConstructor
public class WestNileController {

    private final WestNileService westNileService;

    @GetMapping("/annual")
    public ResponseEntity<ApiResponse<List<WestNileAnnualResponse>>> getAnnualTrend() {
        return ResponseEntity.ok(ApiResponse.ok(westNileService.getAnnualTrend()));
    }

    @GetMapping("/annual/{year}")
    public ResponseEntity<ApiResponse<WestNileAnnualResponse>> getByYear(
            @PathVariable Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(westNileService.getByYear(year)));
    }

    @GetMapping("/annual/range")
    public ResponseEntity<ApiResponse<List<WestNileAnnualResponse>>> getYearRange(
            @RequestParam Integer start,
            @RequestParam Integer end) {
        return ResponseEntity.ok(ApiResponse.ok(westNileService.getYearRange(start, end)));
    }

    @GetMapping("/hospitalizations")
    public ResponseEntity<ApiResponse<List<WestNileHospitalizationResponse>>> getHospitalizations() {
        return ResponseEntity.ok(ApiResponse.ok(westNileService.getAllHospitalizations()));
    }

    @GetMapping("/states")
    public ResponseEntity<ApiResponse<List<WestNileStateResponse>>> getStateData(
            @RequestParam String caseType,
            @RequestParam String yearRange) {
        return ResponseEntity.ok(ApiResponse.ok(westNileService.getStatesByCaseType(caseType, yearRange)));
    }

    @GetMapping("/states/ranges")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableYearRanges() {
        return ResponseEntity.ok(ApiResponse.ok(westNileService.getAvailableYearRanges()));
    }

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<Object[]>>> getMonthlyCases(
            @RequestParam(defaultValue = "Neuroinvasive") String caseType) {
        return ResponseEntity.ok(ApiResponse.ok(westNileService.getMonthlyCaseSums(caseType)));
    }

    @GetMapping("/demographics")
    public ResponseEntity<ApiResponse<List<Object[]>>> getDemographics() {
        return ResponseEntity.ok(ApiResponse.ok(westNileService.getDemographics()));
    }
}
