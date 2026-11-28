package com.nurierdem.vehicle_service.service;

import com.nurierdem.vehicle_service.dto.MaintenancePlanResponse;
import com.nurierdem.vehicle_service.dto.MaintenancePlanResponse.Schedule;
import com.nurierdem.vehicle_service.dto.MaintenancePlanResponse.Status;
import com.nurierdem.vehicle_service.dto.VehicleResponse;
import com.nurierdem.vehicle_service.entity.ServiceRecord;
import com.nurierdem.vehicle_service.enums.ServiceType;
import com.nurierdem.vehicle_service.repository.ServiceRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class MaintenancePlanService {
    private static final int REMINDER_DAYS = 30;
    private static final int REMINDER_KILOMETERS = 1000;

    private final VehicleService vehicleService;
    private final ServiceRecordRepository recordRepository;
    private final int intervalMonths;
    private final int intervalKilometers;

    public MaintenancePlanService(
            VehicleService vehicleService,
            ServiceRecordRepository recordRepository,
            @Value("${vehicle-service.maintenance.interval-months:12}") int intervalMonths,
            @Value("${vehicle-service.maintenance.interval-kilometers:15000}") int intervalKilometers
    ) {
        if (intervalMonths < 1 || intervalKilometers < 1) {
            throw new IllegalArgumentException("Bakım aralıkları pozitif olmalıdır.");
        }
        this.vehicleService = vehicleService;
        this.recordRepository = recordRepository;
        this.intervalMonths = intervalMonths;
        this.intervalKilometers = intervalKilometers;
    }

    public MaintenancePlanResponse getOwnerPlan(Long vehicleId, Long ownerId) {
        return getPlan(vehicleService.getVehicleById(vehicleId, ownerId));
    }

    public MaintenancePlanResponse getServicePlan(Long vehicleId) {
        return getPlan(vehicleService.getServiceVehicle(vehicleId));
    }

    private MaintenancePlanResponse getPlan(VehicleResponse vehicle) {
        LocalDate today = LocalDate.now();
        Schedule schedule = recordRepository
                .findFirstByVehicle_IdAndServiceTypeOrderByServiceDateDescMileageDescIdDesc(
                        vehicle.id(), ServiceType.MAINTENANCE)
                .map(record -> calculate(record, vehicle.currentMileage(), today))
                .orElse(null);
        return new MaintenancePlanResponse(
                intervalMonths, intervalKilometers, REMINDER_DAYS, REMINDER_KILOMETERS, schedule);
    }

    private Schedule calculate(ServiceRecord lastMaintenance, int currentMileage, LocalDate today) {
        LocalDate nextDate = lastMaintenance.getServiceDate().plusMonths(intervalMonths);
        long nextMileage = (long) lastMaintenance.getMileage() + intervalKilometers;
        long days = ChronoUnit.DAYS.between(today, nextDate);
        long kilometers = nextMileage - currentMileage;
        return new Schedule(lastMaintenance.getServiceDate(), lastMaintenance.getMileage(),
                nextDate, nextMileage, days, kilometers, getStatus(days, kilometers));
    }

    private Status getStatus(long days, long kilometers) {
        if (days < 0 || kilometers < 0) {
            return Status.OVERDUE;
        }
        if (days == 0 || kilometers == 0) {
            return Status.DUE;
        }
        if (days <= REMINDER_DAYS || kilometers <= REMINDER_KILOMETERS) {
            return Status.APPROACHING;
        }
        return Status.ON_TRACK;
    }
}
