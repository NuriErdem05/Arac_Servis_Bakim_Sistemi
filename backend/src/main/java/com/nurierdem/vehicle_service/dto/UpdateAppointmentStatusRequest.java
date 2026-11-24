package com.nurierdem.vehicle_service.dto;

import com.nurierdem.vehicle_service.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAppointmentStatusRequest(
        @NotNull(message = "Randevu durumu zorunludur.")
        AppointmentStatus status
) {
}
