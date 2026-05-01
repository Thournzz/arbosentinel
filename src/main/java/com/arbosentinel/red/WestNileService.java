package com.arbosentinel.red;

// ================================================
// RED layer — West Nile business logic
// Powers: West Nile section of Surveillance page
// Data: CDC (annual, hospitalizations, state, monthly, demographics)
// ================================================

import com.arbosentinel.blue.entity.WestNileStateCase;
import com.arbosentinel.green.dto.WestNileAnnualResponse;
import com.arbosentinel.green.dto.WestNileHospitalizationResponse;
import com.arbosentinel.green.dto.WestNileStateResponse;
import com.arbosentinel.green.mapper.WestNileMapper;
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
public class WestNileService {

    private final WestNileAnnualCaseRepository annualRepo;
    private final WestNileHospitalizationRepository hospRepo;
    private final WestNileStateCaseRepository stateRepo;
    private final WestNileMonthlyRepository monthlyRepo;
    private final WestNileDemographicRepository demographicRepo;
    private final WestNileMapper mapper;

    // ── Annual trend ─────────────────────────────────────────────

    @Cacheable(CacheConfig.WEST_NILE_TREND)
    public List<WestNileAnnualResponse> getAnnualTrend() {
        return mapper.toAnnualResponseList(annualRepo.findAllByOrderByYearAsc());
    }

    public WestNileAnnualResponse getByYear(Integer year) {
        return annualRepo.findByYear(year)
            .map(mapper::toAnnualResponse)
            .orElseThrow(() -> new ResourceNotFoundException("WestNileAnnualCase", "year", year));
    }

    public List<WestNileAnnualResponse> getYearRange(Integer start, Integer end) {
        return mapper.toAnnualResponseList(
            annualRepo.findByYearBetweenOrderByYearAsc(start, end));
    }

    // ── Hospitalizations ─────────────────────────────────────────

    public List<WestNileHospitalizationResponse> getAllHospitalizations() {
        return mapper.toHospResponseList(hospRepo.findAllByOrderByYearAsc());
    }

    // ── State-level data ─────────────────────────────────────────

    public List<WestNileStateResponse> getStatesByCaseType(String caseType, String yearRange) {
        return stateRepo.findByTypeAndRangeOrderByCasesDesc(caseType, yearRange)
            .stream()
            .map(s -> new WestNileStateResponse(
                s.getId(), s.getCaseType(), s.getYearRange(), s.getStateCode(), s.getReportedCases()
            ))
            .collect(Collectors.toList());
    }

    public List<String> getAvailableYearRanges() {
        return stateRepo.findDistinctYearRanges();
    }

    // ── Monthly seasonal pattern ─────────────────────────────────

    public List<Object[]> getMonthlyCaseSums(String caseType) {
        return monthlyRepo.sumCasesByMonth(caseType);
    }

    // ── Demographic breakdown ────────────────────────────────────

    public List<Object[]> getDemographics() {
        return demographicRepo.findAllByOrderByAgeGroupAsc()
            .stream()
            .map(d -> new Object[]{ d.getAgeGroup(), d.getMaleRate(), d.getFemaleRate() })
            .collect(java.util.stream.Collectors.toList());
    }
}
