package com.nurierdem.vehicle_service.repository;

import com.nurierdem.vehicle_service.entity.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {
    List<ServiceRecord>findAllByVehicle_IdOrderByServiceDateDescIdDesc(Long vehicleId);
    boolean existsByAppointment_Id(Long appointmentId);
}
