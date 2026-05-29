package com.arbosentinel.blue.controller;

// ══════════════════════════════════════════════════════════════════════════════
// BLUE layer — @RestController
// HTTP entry point for all PAHO Caribbean surveillance endpoints.
//
// LEARNING NOTE — What a Controller does (and does NOT do):
//   The controller's ONLY job is:
//   1. Receive an HTTP request
//   2. Extract parameters (path variables, query params, request body)
//   3. Call the appropriate service method
//   4. Wrap the result in an ApiResponse and return it
//
//   The controller NEVER contains:
//   - Business logic (no if statements about data values)
//   - Database queries (no repository calls directly)
//   - Data transformation (no mapping from entity to DTO — that's the service)
//
//   This strict separation means you can test the service independently of HTTP,
//   and change the HTTP API without touching the business logic.
//
// LEARNING NOTE — @RestController vs @Controller:
//   @Controller returns VIEW NAMES (for Thymeleaf/JSP server-side rendering).
//   @RestController = @Controller + @ResponseBody on every method.
//   @ResponseBody tells Spring to serialise the return value directly to JSON
//   instead of treating it as a view name. Since this is a REST API (not server-side
//   HTML rendering), we always use @RestController.
//
// LEARNING NOTE — @RequestMapping("/api/paho") at class level:
//   Every endpoint in this class has "/api/paho" as a prefix.
//   So @GetMapping("/caribbean") actually maps to GET /api/paho/caribbean.
//   The class-level @RequestMapping groups all PAHO endpoints under one URL prefix.
//
// LEARNING NOTE — ResponseEntity<ApiResponse<T>>:
//   ResponseEntity wraps ANY HTTP response — it gives us control over the status code.
//   ApiResponse<T> is our { success, message, data } wrapper — every API response
//   in ArboSentinel uses this shape so the frontend has a consistent structure to parse.
//   ApiResponse.ok(data) sets success=true, message="OK", data=<your data>.
//   ResponseEntity.ok(...) sets HTTP status 200 and sets the body.
// ══════════════════════════════════════════════════════════════════════════════

import com.arbosentinel.green.dto.ApiResponse;
import com.arbosentinel.green.dto.PahoCaribCaseResponse;
import com.arbosentinel.red.PahoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/paho")
@RequiredArgsConstructor
public class PahoController {

    private final PahoService pahoService;

    // ── Country-specific endpoints ─────────────────────────────────────────────

    // All records for a specific country — full history
    // Example: GET /api/paho/country/jm
    //
    // LEARNING NOTE — @PathVariable:
    //   @PathVariable extracts a value from the URL path.
    //   In "/country/{locationCode}", {locationCode} is a template variable.
    //   Spring maps whatever string appears at that position in the URL to locationCode.
    //   GET /api/paho/country/jm → locationCode = "jm"
    //   GET /api/paho/country/tt → locationCode = "tt"
    @GetMapping("/country/{locationCode}")
    public ResponseEntity<ApiResponse<List<PahoCaribCaseResponse>>> getByCountry(
            @PathVariable String locationCode) {
        return ResponseEntity.ok(ApiResponse.ok(pahoService.getByCountry(locationCode)));
    }

    // Country + year filter — for annual trend charts
    // Example: GET /api/paho/country/jm/2023
    @GetMapping("/country/{locationCode}/{year}")
    public ResponseEntity<ApiResponse<List<PahoCaribCaseResponse>>> getByCountryAndYear(
            @PathVariable String locationCode,
            @PathVariable Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(pahoService.getByCountryAndYear(locationCode, year)));
    }

    // ── Regional/multi-country endpoints ─────────────────────────────────────

    // Most recent week for all 10 Caribbean countries — surveillance dashboard overview
    // Example: GET /api/paho/latest
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<List<PahoCaribCaseResponse>>> getLatestAllCountries() {
        return ResponseEntity.ok(ApiResponse.ok(pahoService.getLatestAllCountries()));
    }

    // Annual totals for all countries in a given year — regional bar chart
    // Example: GET /api/paho/annual?year=2023
    //
    // LEARNING NOTE — @RequestParam:
    //   @RequestParam extracts values from the URL query string (?key=value).
    //   defaultValue = "2023" means if the caller doesn't pass ?year=...,
    //   the method uses 2023 as the year. This prevents a NullPointerException
    //   when year is missing and makes the parameter optional for the caller.
    @GetMapping("/annual")
    public ResponseEntity<ApiResponse<List<Object[]>>> getAnnualTotals(
            @RequestParam(defaultValue = "2023") Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(pahoService.getAnnualTotalsByCountry(year)));
    }

    // All years available in the dataset — for the year-selector UI dropdown
    // Example: GET /api/paho/years
    @GetMapping("/years")
    public ResponseEntity<ApiResponse<List<Integer>>> getAvailableYears() {
        return ResponseEntity.ok(ApiResponse.ok(pahoService.getAvailableYears()));
    }

    // ── Jamaica-specific KPI endpoints ───────────────────────────────────────

    // Latest Jamaica epiweek record — for the real-time dashboard KPI card
    // Example: GET /api/paho/jamaica/latest
    @GetMapping("/jamaica/latest")
    public ResponseEntity<ApiResponse<PahoCaribCaseResponse>> getJamaicaLatest() {
        return ResponseEntity.ok(ApiResponse.ok(pahoService.getJamaicaLatest()));
    }

    // Jamaica all-time total dengue cases — KPI number on dashboard
    // Example: GET /api/paho/jamaica/total
    @GetMapping("/jamaica/total")
    public ResponseEntity<ApiResponse<Long>> getJamaicaTotal() {
        return ResponseEntity.ok(ApiResponse.ok(pahoService.getJamaicaTotalCases()));
    }

    // ── Data coverage / admin ────────────────────────────────────────────────

    // How many rows we have per country — lets admin verify ETL ran successfully
    // Example: GET /api/paho/coverage
    @GetMapping("/coverage")
    public ResponseEntity<ApiResponse<List<Object[]>>> getCoverage() {
        return ResponseEntity.ok(ApiResponse.ok(pahoService.getCountryCoverage()));
    }

    // ── ETL trigger endpoint ──────────────────────────────────────────────────

    // Manual ETL refresh — fetches new epiweeks from Delphi API for all countries
    // Example: POST /api/paho/etl/refresh
    //
    // LEARNING NOTE — Why POST not GET for an ETL trigger?
    //   HTTP GET is IDEMPOTENT and SAFE — it should not change server state.
    //   GET requests can be cached, retried, and pre-fetched by browsers.
    //   Our ETL trigger CHANGES STATE (it inserts new rows into the DB),
    //   so it must be POST. A POST tells the caller: "this has side effects."
    //   Using GET for a write operation is an anti-pattern that can cause
    //   accidental data writes from browser prefetching or cache refresh.
    //
    // LEARNING NOTE — @PreAuthorize vs Security config:
    //   This endpoint is protected by Spring Security — only authenticated users
    //   with a valid JWT can call it. The security config (SecurityConfig.java)
    //   defines which paths require authentication. ETL endpoints should always
    //   be protected to prevent external actors from triggering expensive operations.
    @PostMapping("/etl/refresh")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> triggerRefresh() {
        int inserted = pahoService.triggerRefresh();
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("newRowsInserted", inserted)
        ));
    }
}
