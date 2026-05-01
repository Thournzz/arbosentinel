package com.arbosentinel.green.dto;

import java.time.LocalDateTime;

// ================================================
// GREEN layer — Mr. Prog alert DTO
// Served to the persistent assistant widget on all pages
// alertStatus drives UI colour: info=blue, moderate=yellow,
//                               high=orange, critical=red
// ================================================

public record MrProgAlertResponse(
        Integer id,
        String diseaseType,
        String alertStatus,
        String alertMessage,
        String regionName,
        LocalDateTime triggeredAt,
        LocalDateTime expiresAt
) {}
