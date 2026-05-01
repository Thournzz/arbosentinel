package com.arbosentinel.green.mapper;

import com.arbosentinel.blue.entity.MalariaEstimatedCase;
import com.arbosentinel.green.dto.MalariaEstimatedResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MalariaMapper {

    MalariaEstimatedResponse toResponse(MalariaEstimatedCase entity);

    List<MalariaEstimatedResponse> toResponseList(List<MalariaEstimatedCase> entities);
}
