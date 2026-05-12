package com.arbosentinel.red;

// ================================================
// RED layer — ML microservice integration
// Calls Python FastAPI (scikit-learn dengue model)
// via YELLOW WebClient bean
// Stores results in ml_predictions table (PURPLE)
// Falls back gracefully if Python service is offline
// ================================================

import com.arbosentinel.blue.entity.MlPrediction;
import com.arbosentinel.blue.entity.enums.DiseaseType;
import com.arbosentinel.green.dto.MlPredictionResponse;
import com.arbosentinel.green.mapper.AlertMapper;
import com.arbosentinel.purple.MlPredictionRepository;
import com.arbosentinel.white.MlServiceException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MlService {

    private final WebClient mlServiceClient;
    private final MlPredictionRepository predictionRepo;
    private final AlertMapper alertMapper;
    private final ObjectMapper objectMapper;

    // ── Request/response DTOs for Python FastAPI ─────────────────

    public record DenguePredictRequest(
        String city,
        Integer weekOfYear,
        Double avgTempC,
        Double precipMm,
        Double humidityPct,
        Double ndviNe
    ) {}

    public record DenguePredictResponse(
        Integer predictedCases,
        Double riskScore,
        Double confidencePct,
        @JsonProperty("model") String modelVersion   // FastAPI sends "model" not "modelVersion"
    ) {}

    // ── Prediction request (sync — called from controller) ───────

    @Transactional
    public MlPredictionResponse predictDengueCases(
            String city, Integer weekOfYear, Double avgTempC,
            Double precipMm, Double humidityPct, Double ndviNe) {

        DenguePredictRequest request = new DenguePredictRequest(
            city, weekOfYear, avgTempC, precipMm, humidityPct, ndviNe
        );

        log.info("Requesting dengue prediction: city={} week={}", city, weekOfYear);

        try {
            DenguePredictResponse response = mlServiceClient
                .post()
                .uri("/predict/dengue")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DenguePredictResponse.class)
                .block();

            if (response == null) {
                throw new MlServiceException("ML service returned null response");
            }

            MlPrediction prediction = MlPrediction.builder()
                .diseaseType(DiseaseType.dengue)
                .regionName(cityToRegion(city))
                .predictionDate(LocalDateTime.now())
                .weekOfYear(weekOfYear)
                .avgTempC(BigDecimal.valueOf(avgTempC))
                .precipitationMm(BigDecimal.valueOf(precipMm))
                .humidityPercent(BigDecimal.valueOf(humidityPct))
                .ndvi(BigDecimal.valueOf(ndviNe))
                .predictedCases(response.predictedCases())
                .riskScore(response.riskScore() != null
                    ? BigDecimal.valueOf(response.riskScore()) : null)
                .confidencePercent(response.confidencePct() != null
                    ? BigDecimal.valueOf(response.confidencePct()) : null)
                .modelVersion(response.modelVersion())
                .inputPayload(toJson(request))
                .build();

            MlPrediction saved = predictionRepo.save(prediction);
            log.info("Dengue prediction stored: {} cases predicted (week {})",
                response.predictedCases(), weekOfYear);

            return alertMapper.toPredictionResponse(saved);

        } catch (WebClientResponseException e) {
            log.error("ML service HTTP error {}: {}", e.getStatusCode(), e.getMessage());
            throw new MlServiceException("ML service error: " + e.getStatusCode());
        } catch (Exception e) {
            if (e instanceof MlServiceException) throw e;
            log.error("ML service unavailable: {}", e.getMessage());
            throw new MlServiceException("ML service unavailable — check Python FastAPI is running", e);
        }
    }

    // ── Latest predictions (read) ────────────────────────────────

    public List<MlPredictionResponse> getLatestPredictions(String diseaseType, int limit) {
        DiseaseType type = DiseaseType.valueOf(diseaseType.toLowerCase());
        var pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        return alertMapper.toPredictionResponseList(
            predictionRepo.findLatestPredictions(type, pageable)
        );
    }

    public List<MlPredictionResponse> getHighRiskPredictions(double threshold) {
        return alertMapper.toPredictionResponseList(
            predictionRepo.findHighRiskPredictions(threshold)
        );
    }

    // ── Async prediction for background scheduling ───────────────

    @Async
    public CompletableFuture<MlPredictionResponse> predictAsync(
            String city, Integer weekOfYear, Double avgTempC,
            Double precipMm, Double humidityPct, Double ndviNe) {
        try {
            return CompletableFuture.completedFuture(
                predictDengueCases(city, weekOfYear, avgTempC, precipMm, humidityPct, ndviNe)
            );
        } catch (Exception e) {
            log.warn("Async ML prediction failed: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private String cityToRegion(String city) {
        return switch (city.toLowerCase()) {
            case "sj" -> "San Juan, Puerto Rico";
            case "iq" -> "Iquitos, Peru";
            default   -> city;
        };
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
