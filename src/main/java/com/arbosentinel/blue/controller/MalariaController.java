package com.arbosentinel.blue.controller;

import com.arbosentinel.green.dto.*;
import com.arbosentinel.red.MalariaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/malaria")
@RequiredArgsConstructor
public class MalariaController {

    private final MalariaService malariaService;

    @GetMapping("/burden")
    public ResponseEntity<ApiResponse<List<MalariaBurdenResponse>>> getGlobalBurden() {
        return ResponseEntity.ok(ApiResponse.ok(malariaService.getGlobalBurdenByYear()));
    }

    @GetMapping("/regions")
    public ResponseEntity<ApiResponse<List<Object[]>>> getBurdenByRegion() {
        return ResponseEntity.ok(ApiResponse.ok(malariaService.getBurdenByRegion()));
    }

    @GetMapping("/top/{year}")
    public ResponseEntity<ApiResponse<List<MalariaEstimatedResponse>>> getTopCountriesByYear(
            @PathVariable Integer year,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(malariaService.getTopCountriesByYear(year, limit)));
    }

    @GetMapping("/country/{country}")
    public ResponseEntity<ApiResponse<List<MalariaEstimatedResponse>>> getCountryHistory(
            @PathVariable String country) {
        return ResponseEntity.ok(ApiResponse.ok(malariaService.getCountryHistory(country)));
    }

    @GetMapping("/country/{country}/{year}")
    public ResponseEntity<ApiResponse<MalariaEstimatedResponse>> getCountryYear(
            @PathVariable String country,
            @PathVariable Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(malariaService.getCountryYear(country, year)));
    }

    @GetMapping("/year/{year}")
    public ResponseEntity<ApiResponse<PagedResponse<MalariaEstimatedResponse>>> getByYear(
            @PathVariable Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(malariaService.getByYear(year, page, size)));
    }
}
