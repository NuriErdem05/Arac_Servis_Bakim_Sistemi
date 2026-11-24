package com.nurierdem.vehicle_service.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateAppointmentRequest(
        @NotNull(message = "Araç sahibi zorunludur.")
        @Positive(message = "Araç sahibi kimliği pozitif olmalıdır.")
        Long ownerId,

        @NotNull(message = "Araç seçimi zorunludur.")
        @Positive(message = "Araç kimliği pozitif olmalıdır.")
        Long vehicleId,

        @NotNull(message = "Randevu tarihi ve saati zorunludur.")
        @Future(message = "Randevu gelecekte bir tarih ve saatte olmalıdır.")
        LocalDateTime appointmentDateTime,

        @NotBlank(message = "Talep açıklaması zorunludur.")
        @Size(max = 1000, message = "Talep açıklaması en fazla 1000 karekter olabilir.")
        String requestDescription
) {
}
