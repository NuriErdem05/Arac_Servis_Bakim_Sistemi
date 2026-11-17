package com.nurierdem.vehicle_service.dto;

import com.nurierdem.vehicle_service.enums.FuelType;

public record VehicleResponse(
        Long id,
        Long ownerId,
        String plate,
        String brand,
        String model,
        Integer modelYear,
        FuelType fuelType,
        String engineInfo,
        Integer currentMileage
) {
}
