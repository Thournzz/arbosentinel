package com.arbosentinel.red;

import com.arbosentinel.green.dto.PagedResponse;
import com.arbosentinel.green.dto.ZikaCaseResponse;
import com.arbosentinel.green.dto.ZikaLocationSummaryResponse;
import com.arbosentinel.green.mapper.ZikaMapper;
import com.arbosentinel.purple.ZikaCaseRepository;
import com.arbosentinel.yellow.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ZikaService {

    private final ZikaCaseRepository zikaRepo;
    private final ZikaMapper zikaMapper;

    @Cacheable(CacheConfig.ZIKA_LOCATIONS)
    public List<ZikaLocationSummaryResponse> getLocationSummary() {
        return zikaRepo.sumConfirmedCasesByLocation()
            .stream()
            .map(row -> new ZikaLocationSummaryResponse(
                (String) row[0],
                row[1] != null ? ((Number) row[1]).longValue() : null
            ))
            .collect(Collectors.toList());
    }

    @Cacheable(CacheConfig.ZIKA_LOCATIONS)
    public List<String> getDistinctLocations() {
        return zikaRepo.findDistinctLocations();
    }

    public PagedResponse<ZikaCaseResponse> getConfirmedCases(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("reportDate").descending());
        return PagedResponse.from(
            zikaRepo.findConfirmedCases(pageable).map(zikaMapper::toResponse)
        );
    }

    public PagedResponse<ZikaCaseResponse> getByLocation(String location, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("reportDate").descending());
        return PagedResponse.from(
            zikaRepo.findByLocation(location, pageable).map(zikaMapper::toResponse)
        );
    }
}
