package com.arbosentinel.green.mapper;

import com.arbosentinel.blue.entity.DengueWeeklyCase;
import com.arbosentinel.green.dto.DengueWeeklyCaseResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DengueMapper {

    // All field names match entity → record — MapStruct maps directly
    DengueWeeklyCaseResponse toResponse(DengueWeeklyCase entity);

    List<DengueWeeklyCaseResponse> toResponseList(List<DengueWeeklyCase> entities);
}
