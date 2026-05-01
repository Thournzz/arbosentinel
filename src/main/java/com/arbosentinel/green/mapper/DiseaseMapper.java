package com.arbosentinel.green.mapper;

import com.arbosentinel.blue.entity.Disease;
import com.arbosentinel.green.dto.DiseaseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DiseaseMapper {

    // diseaseType is an enum — map to its name() string for JSON
    @Mapping(target = "diseaseType", expression = "java(disease.getDiseaseType().name())")
    DiseaseResponse toResponse(Disease disease);

    List<DiseaseResponse> toResponseList(List<Disease> diseases);
}
