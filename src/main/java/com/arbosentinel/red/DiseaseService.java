package com.arbosentinel.red;

// ================================================
// RED layer — Disease & vector business logic
// Powers: Pathogen Library page (all 5 diseases)
// Assembles DiseaseDetailResponse from 3 tables:
//   diseases + vectors (via disease_vectors) + symptoms + drugs
// ================================================

import com.arbosentinel.blue.entity.Disease;
import com.arbosentinel.blue.entity.Vector;
import com.arbosentinel.blue.entity.enums.DiseaseType;
import com.arbosentinel.green.dto.*;
import com.arbosentinel.green.mapper.DiseaseMapper;
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
public class DiseaseService {

    private final DiseaseRepository diseaseRepository;
    private final VectorRepository vectorRepository;
    private final DiseaseVectorRepository diseaseVectorRepository;
    private final ClinicalSymptomProfileRepository symptomRepository;
    private final PharmacologyDrugRepository drugRepository;
    private final DrugDiseaseIndicationRepository indicationRepository;
    private final DiseaseMapper diseaseMapper;
    private final PharmacologyMapper pharmacologyMapper;

    // ── Disease list ────────────────────────────────────────────

    @Cacheable(CacheConfig.DISEASES)
    public List<DiseaseResponse> getAllDiseases() {
        log.debug("Fetching all disease profiles");
        return diseaseMapper.toResponseList(diseaseRepository.findAll());
    }

    @Cacheable(value = CacheConfig.DISEASE_DETAIL, key = "#diseaseType")
    public DiseaseDetailResponse getDiseaseDetail(String diseaseType) {
        DiseaseType type = parseDiseaseType(diseaseType);
        Disease disease = diseaseRepository.findByDiseaseType(type)
            .orElseThrow(() -> new ResourceNotFoundException("Disease", "type", diseaseType));

        // Vectors with isPrimary + notes from join table
        List<VectorResponse> vectors = buildVectorResponses(disease.getId());

        // Clinical symptoms
        List<ClinicalSymptomResponse> symptoms = pharmacologyMapper
            .toSymptomResponseList(
                symptomRepository.findByDiseaseIdOrderByPrevalencePercentDesc(disease.getId())
            );

        // Drugs with indications for this disease
        List<PharmacologyDrugResponse> drugs = buildDrugResponses(disease.getId());

        return new DiseaseDetailResponse(
            disease.getId(),
            disease.getDiseaseType().name(),
            disease.getCommonName(),
            disease.getPathogenFamily(),
            disease.getPathogenSpecies(),
            disease.getGenomeType(),
            disease.getStructure(),
            disease.getTransmissionRoute(),
            disease.getFirstIdentifiedYear(),
            disease.getFirstIdentifiedLocation(),
            disease.getWhoClassification(),
            disease.getIncubationMinDays(),
            disease.getIncubationMaxDays(),
            disease.getAcutePhaseDescription(),
            disease.getComplications(),
            disease.getRecoveryDescription(),
            disease.getTreatmentSummary(),
            vectors,
            symptoms,
            drugs
        );
    }

    // ── Vectors ─────────────────────────────────────────────────

    @Cacheable(CacheConfig.VECTORS)
    public List<VectorResponse> getAllVectors() {
        return vectorRepository.findAll().stream()
            .map(v -> new VectorResponse(
                v.getId(), v.getGenus(), v.getSpecies(), v.getCommonName(),
                v.getGeographicRange(), v.getBreedingConditions(), v.getActivityPeak(),
                null, null
            ))
            .collect(Collectors.toList());
    }

    // ── Internal helpers ─────────────────────────────────────────

    private List<VectorResponse> buildVectorResponses(Integer diseaseId) {
        return diseaseVectorRepository.findById_DiseaseId(diseaseId)
            .stream()
            .map(dv -> {
                Vector v = vectorRepository.findById(dv.getId().getVectorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Vector", "id", dv.getId().getVectorId()));
                return new VectorResponse(
                    v.getId(), v.getGenus(), v.getSpecies(), v.getCommonName(),
                    v.getGeographicRange(), v.getBreedingConditions(), v.getActivityPeak(),
                    dv.getIsPrimary(), dv.getNotes()
                );
            })
            .collect(Collectors.toList());
    }

    private List<PharmacologyDrugResponse> buildDrugResponses(Integer diseaseId) {
        return drugRepository.findByDiseaseId(diseaseId)
            .stream()
            .map(drug -> {
                List<DrugIndicationResponse> indications = pharmacologyMapper
                    .toIndicationResponseList(
                        indicationRepository.findByDiseaseIdAndIndicationType(
                            diseaseId, "treatment")
                    );
                // Return drug with indications for this specific disease context
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

    private DiseaseType parseDiseaseType(String raw) {
        try {
            return DiseaseType.valueOf(raw.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(
                "Invalid disease type: '" + raw + "'. Valid: dengue, malaria, zika, west_nile, chikungunya");
        }
    }
}
