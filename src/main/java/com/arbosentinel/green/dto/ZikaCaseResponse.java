package com.arbosentinel.green.dto;

import java.time.LocalDate;

public record ZikaCaseResponse(
        Integer id,
        LocalDate reportDate,
        String location,
        String locationType,
        String dataField,
        String dataFieldCode,
        String timePeriod,
        String timePeriodType,
        Integer value,
        String unit
) {}
