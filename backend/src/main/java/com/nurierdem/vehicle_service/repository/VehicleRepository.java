package com.nurierdem.vehicle_service.repository;

import com.nurierdem.vehicle_service.entity.Vehicle;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle,Long> {
    List<Vehicle> findAllByArchivedFalseOrderByIdDesc();

    Optional<Vehicle> findByIdAndArchivedFalse(Long id);

    List<Vehicle> findAllByOwner_IdAndArchivedFalseOrderByIdDesc(Long ownerId);

    Optional<Vehicle> findByIdAndOwner_IdAndArchivedFalse(Long id, Long ownerId);

    boolean existsByPlate(String plate);
    boolean existsByPlateAndIdNot(String plate, Long id); //Aynı plaka başka bir araçta var mı yok mu

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.id = :id and v.archived = false")
    Optional<Vehicle> findActiveByIdForUpdate(@Param("id") Long id);
}
