package com.nurierdem.vehicle_service.dto;

import com.nurierdem.vehicle_service.enums.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long vehicleId,
        String plate,
        LocalDateTime appointmentDateTime,
        String requestDescription,
        AppointmentStatus status,
        Long assignedStaffId,
        String assignedStaffName
) {
}
