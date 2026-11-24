package com.nurierdem.vehicle_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignAppointmentStaffRequest(
        @NotNull(message = "Personel seçimi zorunludur.")
        @Positive(message = "Personel kimliği pozitif olmalıdır.")
        Long staffId
) {
}
