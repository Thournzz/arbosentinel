package com.arbosentinel.green.mapper;

import com.arbosentinel.blue.entity.ClinicalSymptomProfile;
import com.arbosentinel.blue.entity.DrugDiseaseIndication;
import com.arbosentinel.blue.entity.PharmacologyDrug;
import com.arbosentinel.green.dto.ClinicalSymptomResponse;
import com.arbosentinel.green.dto.DrugIndicationResponse;
import com.arbosentinel.green.dto.PharmacologyDrugResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PharmacologyMapper {

    // Map drug without indications list (indications field = null)
    @Mapping(target = "indications", ignore = true)
    PharmacologyDrugResponse toResponse(PharmacologyDrug entity);

    List<PharmacologyDrugResponse> toResponseList(List<PharmacologyDrug> entities);

    DrugIndicationResponse toIndicationResponse(DrugDiseaseIndication entity);

    List<DrugIndicationResponse> toIndicationResponseList(List<DrugDiseaseIndication> entities);

    ClinicalSymptomResponse toSymptomResponse(ClinicalSymptomProfile entity);

    List<ClinicalSymptomResponse> toSymptomResponseList(List<ClinicalSymptomProfile> entities);
}
