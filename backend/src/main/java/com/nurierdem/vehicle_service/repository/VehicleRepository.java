package com.nurierdem.vehicle_service.repository;

import com.nurierdem.vehicle_service.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle,Long> {
    List<Vehicle> findAllByOwner_IdAndArchivedFalseOrderByIdDesc(Long ownerId);

    Optional<Vehicle> findByIdAndOwner_IdAndArchivedFalse(Long id, Long ownerId);

    boolean existsByPlate(String plate);
    boolean existsByPlateAndIdNot(String plate, Long id); //Aynı plaka başka bir araçta var mı yok mu
}
