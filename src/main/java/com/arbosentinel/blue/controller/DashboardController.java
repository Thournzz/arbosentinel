package com.arbosentinel.blue.controller;

import com.arbosentinel.green.dto.*;
import com.arbosentinel.red.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // Hero stats for the landing Surveillance dashboard
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboardStats()));
    }
}
