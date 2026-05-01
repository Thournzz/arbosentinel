package com.arbosentinel.blue.controller;

import com.arbosentinel.green.dto.*;
import com.arbosentinel.red.ZikaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zika")
@RequiredArgsConstructor
public class ZikaController {

    private final ZikaService zikaService;

    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<List<ZikaLocationSummaryResponse>>> getLocationSummary() {
        return ResponseEntity.ok(ApiResponse.ok(zikaService.getLocationSummary()));
    }

    @GetMapping("/locations/list")
    public ResponseEntity<ApiResponse<List<String>>> getDistinctLocations() {
        return ResponseEntity.ok(ApiResponse.ok(zikaService.getDistinctLocations()));
    }

    @GetMapping("/confirmed")
    public ResponseEntity<ApiResponse<PagedResponse<ZikaCaseResponse>>> getConfirmedCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(zikaService.getConfirmedCases(page, size)));
    }

    @GetMapping("/by-location")
    public ResponseEntity<ApiResponse<PagedResponse<ZikaCaseResponse>>> getByLocation(
            @RequestParam String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(zikaService.getByLocation(location, page, size)));
    }
}
