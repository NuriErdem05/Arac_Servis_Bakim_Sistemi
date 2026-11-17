package com.nurierdem.vehicle_service.dto;

import java.util.Map;

public record ApiErrorResponse(
        int status,
        String message,
        Map<String, String> fieldErrors
) {
}
