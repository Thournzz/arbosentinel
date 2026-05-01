package com.arbosentinel.blue.controller;

import com.arbosentinel.green.dto.*;
import com.arbosentinel.red.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    // All active Mr. Prog alerts — polled by the widget
    @GetMapping
    public ResponseEntity<ApiResponse<List<MrProgAlertResponse>>> getActiveAlerts() {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getAllActiveAlerts()));
    }

    // Disease-specific active alerts
    @GetMapping("/disease/{diseaseType}")
    public ResponseEntity<ApiResponse<List<MrProgAlertResponse>>> getAlertsByDisease(
            @PathVariable String diseaseType) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getActiveAlertsForDisease(diseaseType)));
    }

    // High-priority alerts only (high + critical)
    @GetMapping("/priority")
    public ResponseEntity<ApiResponse<List<MrProgAlertResponse>>> getHighPriorityAlerts() {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getHighPriorityAlerts()));
    }

    // Alert badge count by status
    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<List<Object[]>>> getAlertCountByStatus() {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getActiveAlertCountByStatus()));
    }
}
