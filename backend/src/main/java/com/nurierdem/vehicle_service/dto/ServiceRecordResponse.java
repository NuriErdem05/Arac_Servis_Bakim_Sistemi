package com.nurierdem.vehicle_service.dto;

import com.nurierdem.vehicle_service.enums.ServiceType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceRecordResponse(
        Long id,
        Long vehicleId,
        String plate,
        Long appointmentId,
        Long performedById,
        String performedByName,
        ServiceType serviceType,
        LocalDate serviceDate,
        Integer mileage,
        String replacedParts,
        String description,
        BigDecimal cost
) {
}
