package com.arbosentinel.red;

// ================================================
// RED layer — Pharmacology + clinical business logic
// Powers: Pharmacology page
// Note: This page is dedicated to Dr. Simone Sandiford's
// research on vector control compounds + biopesticides
// ================================================

import com.arbosentinel.green.dto.ClinicalSymptomResponse;
import com.arbosentinel.green.dto.DrugIndicationResponse;
import com.arbosentinel.green.dto.PharmacologyDrugResponse;
import com.arbosentinel.green.mapper.PharmacologyMapper;
import com.arbosentinel.purple.*;
import com.arbosentinel.white.ResourceNotFoundException;
import com.arbosentinel.yellow.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PharmacologyService {

    private final PharmacologyDrugRepository drugRepo;
    private final DrugDiseaseIndicationRepository indicationRepo;
    private final ClinicalSymptomProfileRepository symptomRepo;
    private final DiseaseRepository diseaseRepo;
    private final PharmacologyMapper mapper;

    // ── All drugs ────────────────────────────────────────────────

    @Cacheable(CacheConfig.PHARMACOLOGY)
    public List<PharmacologyDrugResponse> getAllDrugs() {
        return mapper.toResponseList(drugRepo.findAll());
    }

    // ── WHO essential medicines only ─────────────────────────────

    public List<PharmacologyDrugResponse> getWhoEssentialDrugs() {
        return mapper.toResponseList(drugRepo.findByWhoEssentialMedicineTrue());
    }

    // ── Drugs with interaction warnings ─────────────────────────

    public List<PharmacologyDrugResponse> getDrugsWithWarnings() {
        return mapper.toResponseList(drugRepo.findByInteractionWarningTrue());
    }

    // ── Drugs for a specific disease ─────────────────────────────

    public List<PharmacologyDrugResponse> getDrugsForDisease(Integer diseaseId) {
        return drugRepo.findByDiseaseId(diseaseId)
            .stream()
            .map(drug -> {
                List<DrugIndicationResponse> indications = mapper
                    .toIndicationResponseList(
                        indicationRepo.findByDiseaseIdAndIndicationType(diseaseId, "treatment")
                    );
                return new PharmacologyDrugResponse(
                    drug.getId(), drug.getDrugName(), drug.getDrugClass(),
                    drug.getMechanismOfAction(), drug.getDosingAdult(),
                    drug.getDosingPediatric(), drug.getKeyInteractions(),
                    drug.getContraindications(), drug.getWhoEssentialMedicine(),
                    drug.getInteractionWarning(), indications
                );
            })
            .collect(Collectors.toList());
    }

    // ── Indications for a drug ───────────────────────────────────

    public List<DrugIndicationResponse> getDrugIndications(Integer drugId) {
        return mapper.toIndicationResponseList(indicationRepo.findByDrugId(drugId));
    }

    // ── Clinical symptoms for a disease ─────────────────────────

    public List<ClinicalSymptomResponse> getSymptomsForDisease(Integer diseaseId) {
        return mapper.toSymptomResponseList(
            symptomRepo.findByDiseaseIdOrderByPrevalencePercentDesc(diseaseId)
        );
    }

    // ── Pathognomonic symptoms only ──────────────────────────────

    public List<ClinicalSymptomResponse> getPathognomonicSymptoms(Integer diseaseId) {
        return mapper.toSymptomResponseList(
            symptomRepo.findByDiseaseIdAndIsPathognomonicTrue(diseaseId)
        );
    }

    // ── All pathognomonic symptoms across all diseases ───────────

    public List<ClinicalSymptomResponse> getAllPathognomonic() {
        return mapper.toSymptomResponseList(
            symptomRepo.findAllPathognomonic()
        );
    }

    // ── Drug lookup by class ─────────────────────────────────────

    public List<PharmacologyDrugResponse> getDrugsByClass(String drugClass) {
        return mapper.toResponseList(drugRepo.findByDrugClass(drugClass));
    }

    // ── Single drug detail ───────────────────────────────────────

    public PharmacologyDrugResponse getDrugById(Integer id) {
        return drugRepo.findById(id)
            .map(drug -> {
                List<DrugIndicationResponse> allIndications = mapper
                    .toIndicationResponseList(indicationRepo.findByDrugId(id));
                return new PharmacologyDrugResponse(
                    drug.getId(), drug.getDrugName(), drug.getDrugClass(),
                    drug.getMechanismOfAction(), drug.getDosingAdult(),
                    drug.getDosingPediatric(), drug.getKeyInteractions(),
                    drug.getContraindications(), drug.getWhoEssentialMedicine(),
                    drug.getInteractionWarning(), allIndications
                );
            })
            .orElseThrow(() -> new ResourceNotFoundException("Drug", "id", id));
    }
}
