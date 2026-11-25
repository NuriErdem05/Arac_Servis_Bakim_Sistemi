package com.nurierdem.vehicle_service.repository;

import com.nurierdem.vehicle_service.entity.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {
    List<ServiceRecord> findAllByOrderByServiceDateDescIdDesc();

    List<ServiceRecord>findAllByVehicle_IdOrderByServiceDateDescIdDesc(Long vehicleId);
    boolean existsByAppointment_Id(Long appointmentId);

    boolean existsByVehicle_IdAndServiceDateBeforeAndMileageGreaterThan(
            Long vehicleId, LocalDate serviceDate, Integer mileage);

    boolean existsByVehicle_IdAndServiceDateAfterAndMileageLessThan(
            Long vehicleId, LocalDate serviceDate, Integer mileage);
}
