package com.nurierdem.vehicle_service.dto;

import java.time.LocalDate;

public record MaintenancePlanResponse(
        int intervalMonths,
        int intervalKilometers,
        int reminderDays,
        int reminderKilometers,
        Schedule schedule
) {
    public record Schedule(
            LocalDate lastMaintenanceDate,
            int lastMaintenanceMileage,
            LocalDate nextMaintenanceDate,
            long nextMaintenanceMileage,
            long daysUntilMaintenance,
            long kilometersUntilMaintenance,
            Status status
    ) {}

    public enum Status {
        ON_TRACK, APPROACHING, DUE, OVERDUE
    }
}
