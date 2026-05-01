package com.arbosentinel.blue.controller;

import com.arbosentinel.green.dto.*;
import com.arbosentinel.red.DiseaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diseases")
@RequiredArgsConstructor
public class DiseaseController {

    private final DiseaseService diseaseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DiseaseResponse>>> getAllDiseases() {
        return ResponseEntity.ok(ApiResponse.ok(diseaseService.getAllDiseases()));
    }

    @GetMapping("/{diseaseType}")
    public ResponseEntity<ApiResponse<DiseaseDetailResponse>> getDiseaseDetail(
            @PathVariable String diseaseType) {
        return ResponseEntity.ok(ApiResponse.ok(diseaseService.getDiseaseDetail(diseaseType)));
    }

    @GetMapping("/vectors")
    public ResponseEntity<ApiResponse<List<VectorResponse>>> getAllVectors() {
        return ResponseEntity.ok(ApiResponse.ok(diseaseService.getAllVectors()));
    }
}
