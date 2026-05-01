package com.arbosentinel.blue.controller;

import com.arbosentinel.green.dto.*;
import com.arbosentinel.red.DengueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dengue")
@RequiredArgsConstructor
public class DengueController {

    private final DengueService dengueService;

    // Annual totals per city — powers year-over-year chart
    @GetMapping("/annual")
    public ResponseEntity<ApiResponse<List<DengueAnnualSummaryResponse>>> getAnnualSummary() {
        return ResponseEntity.ok(ApiResponse.ok(dengueService.getAnnualSummary()));
    }

    // City totals (hero numbers)
    @GetMapping("/totals")
    public ResponseEntity<ApiResponse<List<Object[]>>> getCityTotals() {
        return ResponseEntity.ok(ApiResponse.ok(dengueService.getCityTotals()));
    }

    // Paged weekly records for a city
    @GetMapping("/weekly/{city}")
    public ResponseEntity<ApiResponse<PagedResponse<DengueWeeklyCaseResponse>>> getWeeklyByCity(
            @PathVariable String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "52") int size) {
        return ResponseEntity.ok(ApiResponse.ok(dengueService.getWeeklyCasesByCity(city, page, size)));
    }

    // Single week
    @GetMapping("/weekly/{city}/{year}/{week}")
    public ResponseEntity<ApiResponse<DengueWeeklyCaseResponse>> getWeek(
            @PathVariable String city,
            @PathVariable Integer year,
            @PathVariable Integer week) {
        return ResponseEntity.ok(ApiResponse.ok(dengueService.getWeek(city, year, week)));
    }

    // Year range for a city
    @GetMapping("/range/{city}")
    public ResponseEntity<ApiResponse<List<DengueWeeklyCaseResponse>>> getYearRange(
            @PathVariable String city,
            @RequestParam Integer startYear,
            @RequestParam Integer endYear) {
        return ResponseEntity.ok(ApiResponse.ok(dengueService.getYearRange(city, startYear, endYear)));
    }

    // Weekly timeline for a specific city + year
    @GetMapping("/timeline/{city}/{year}")
    public ResponseEntity<ApiResponse<List<Object[]>>> getWeeklyTimeline(
            @PathVariable String city,
            @PathVariable Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(dengueService.getWeeklyTimeline(city, year)));
    }

    // Seasonal peaks per city
    @GetMapping("/seasonal/{city}")
    public ResponseEntity<ApiResponse<List<Object[]>>> getSeasonalPeaks(
            @PathVariable String city) {
        return ResponseEntity.ok(ApiResponse.ok(dengueService.getSeasonalPeaks(city)));
    }
}
