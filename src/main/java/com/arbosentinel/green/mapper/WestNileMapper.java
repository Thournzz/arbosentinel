package com.arbosentinel.green.mapper;

import com.arbosentinel.blue.entity.WestNileAnnualCase;
import com.arbosentinel.blue.entity.WestNileHospitalization;
import com.arbosentinel.green.dto.WestNileAnnualResponse;
import com.arbosentinel.green.dto.WestNileHospitalizationResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WestNileMapper {

    WestNileAnnualResponse toAnnualResponse(WestNileAnnualCase entity);

    List<WestNileAnnualResponse> toAnnualResponseList(List<WestNileAnnualCase> entities);

    WestNileHospitalizationResponse toHospResponse(WestNileHospitalization entity);

    List<WestNileHospitalizationResponse> toHospResponseList(List<WestNileHospitalization> entities);
}
