package com.arbosentinel.blue.controller;

import com.arbosentinel.green.dto.*;
import com.arbosentinel.red.PharmacologyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacology")
@RequiredArgsConstructor
public class PharmacologyController {

    private final PharmacologyService pharmacologyService;

    @GetMapping("/drugs")
    public ResponseEntity<ApiResponse<List<PharmacologyDrugResponse>>> getAllDrugs() {
        return ResponseEntity.ok(ApiResponse.ok(pharmacologyService.getAllDrugs()));
    }

    @GetMapping("/drugs/{id}")
    public ResponseEntity<ApiResponse<PharmacologyDrugResponse>> getDrugById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(pharmacologyService.getDrugById(id)));
    }

    @GetMapping("/drugs/who-essential")
    public ResponseEntity<ApiResponse<List<PharmacologyDrugResponse>>> getWhoEssentialDrugs() {
        return ResponseEntity.ok(ApiResponse.ok(pharmacologyService.getWhoEssentialDrugs()));
    }

    @GetMapping("/drugs/warnings")
    public ResponseEntity<ApiResponse<List<PharmacologyDrugResponse>>> getDrugsWithWarnings() {
        return ResponseEntity.ok(ApiResponse.ok(pharmacologyService.getDrugsWithWarnings()));
    }

    @GetMapping("/drugs/disease/{diseaseId}")
    public ResponseEntity<ApiResponse<List<PharmacologyDrugResponse>>> getDrugsForDisease(
            @PathVariable Integer diseaseId) {
        return ResponseEntity.ok(ApiResponse.ok(pharmacologyService.getDrugsForDisease(diseaseId)));
    }

    @GetMapping("/symptoms/{diseaseId}")
    public ResponseEntity<ApiResponse<List<ClinicalSymptomResponse>>> getSymptomsForDisease(
            @PathVariable Integer diseaseId) {
        return ResponseEntity.ok(ApiResponse.ok(pharmacologyService.getSymptomsForDisease(diseaseId)));
    }

    @GetMapping("/symptoms/pathognomonic")
    public ResponseEntity<ApiResponse<List<ClinicalSymptomResponse>>> getAllPathognomonic() {
        return ResponseEntity.ok(ApiResponse.ok(pharmacologyService.getAllPathognomonic()));
    }
}
