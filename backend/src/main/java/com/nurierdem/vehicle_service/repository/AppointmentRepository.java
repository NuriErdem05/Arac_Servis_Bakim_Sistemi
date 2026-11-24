package com.nurierdem.vehicle_service.repository;

import com.nurierdem.vehicle_service.entity.Appointment;
import com.nurierdem.vehicle_service.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    List<Appointment>
    findAllByVehicle_Owner_IdOrderByAppointmentDateTimeDescIdDesc(
            Long ownerId
    );

    List<Appointment>
    findAllByVehicle_Owner_IdAndStatusOrderByAppointmentDateTimeDescIdDesc(
            Long ownerId,
            AppointmentStatus status
    );

    List<Appointment>
    findAllByAssignedStaff_IdOrderByAppointmentDateTimeDescIdDesc(
            Long staffId
    );

    List<Appointment>
    findAllByAssignedStaff_IdAndStatusOrderByAppointmentDateTimeDescIdDesc(
            Long staffId,
            AppointmentStatus status
    );

    List<Appointment>
    findAllByOrderByAppointmentDateTimeDescIdDesc();

    List<Appointment>
    findAllByStatusOrderByAppointmentDateTimeDescIdDesc(
            AppointmentStatus status
    );

    boolean existsByAppointmentDateTimeAndStatusIn(
            LocalDateTime appointmentDateTime,
            Collection<AppointmentStatus> statuses
    );

    boolean existsByVehicle_IdAndAssignedStaff_IdAndStatusIn(
            Long vehicleId,
            Long staffId,
            Collection<AppointmentStatus> statuses
    );

    @Query(
            value = "SELECT 1 FROM pg_advisory_xact_lock(73101)",
            nativeQuery = true
    )
    Integer lockSchedule();
}