package com.nurierdem.vehicle_service.repository;

import com.nurierdem.vehicle_service.entity.ServiceRecord;
import com.nurierdem.vehicle_service.enums.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {
    List<ServiceRecord> findAllByOrderByServiceDateDescIdDesc();

    List<ServiceRecord>findAllByVehicle_IdOrderByServiceDateDescIdDesc(Long vehicleId);
    Optional<ServiceRecord> findFirstByVehicle_IdAndServiceTypeOrderByServiceDateDescMileageDescIdDesc(
            Long vehicleId, ServiceType serviceType);

    boolean existsByAppointment_Id(Long appointmentId);

    boolean existsByVehicle_IdAndServiceDateBeforeAndMileageGreaterThan(
            Long vehicleId, LocalDate serviceDate, Integer mileage);

    boolean existsByVehicle_IdAndServiceDateAfterAndMileageLessThan(
            Long vehicleId, LocalDate serviceDate, Integer mileage);
}
