package com.arbosentinel.green.mapper;

import com.arbosentinel.blue.entity.ZikaCase;
import com.arbosentinel.green.dto.ZikaCaseResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ZikaMapper {

    ZikaCaseResponse toResponse(ZikaCase entity);

    List<ZikaCaseResponse> toResponseList(List<ZikaCase> entities);
}
