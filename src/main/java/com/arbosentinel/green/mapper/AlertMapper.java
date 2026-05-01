package com.arbosentinel.green.mapper;

import com.arbosentinel.blue.entity.MlPrediction;
import com.arbosentinel.blue.entity.MrProgAlert;
import com.arbosentinel.blue.entity.OutbreakRiskScore;
import com.arbosentinel.green.dto.MlPredictionResponse;
import com.arbosentinel.green.dto.MrProgAlertResponse;
import com.arbosentinel.green.dto.OutbreakRiskResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AlertMapper {

    @Mapping(target = "diseaseType",
             expression = "java(alert.getDiseaseType() != null ? alert.getDiseaseType().name() : null)")
    @Mapping(target = "alertStatus",
             expression = "java(alert.getAlertStatus().name())")
    MrProgAlertResponse toAlertResponse(MrProgAlert alert);

    List<MrProgAlertResponse> toAlertResponseList(List<MrProgAlert> alerts);

    @Mapping(target = "diseaseType",
             expression = "java(score.getDiseaseType().name())")
    @Mapping(target = "riskLevel",
             expression = "java(score.getRiskLevel() != null ? score.getRiskLevel().name() : null)")
    OutbreakRiskResponse toRiskResponse(OutbreakRiskScore score);

    List<OutbreakRiskResponse> toRiskResponseList(List<OutbreakRiskScore> scores);

    @Mapping(target = "diseaseType",
             expression = "java(prediction.getDiseaseType().name())")
    MlPredictionResponse toPredictionResponse(MlPrediction prediction);

    List<MlPredictionResponse> toPredictionResponseList(List<MlPrediction> predictions);
}
