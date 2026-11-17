package com.nurierdem.vehicle_service.service;

import com.nurierdem.vehicle_service.dto.CreateVehicleRequest;
import com.nurierdem.vehicle_service.dto.VehicleResponse;
import com.nurierdem.vehicle_service.entity.User;
import com.nurierdem.vehicle_service.entity.Vehicle;
import com.nurierdem.vehicle_service.repository.UserRepository;
import com.nurierdem.vehicle_service.repository.VehicleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.nurierdem.vehicle_service.dto.UpdateVehicleRequest;

import java.time.Year;
import java.util.List;
import java.util.Locale;

@Service
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public VehicleResponse createVehicle(CreateVehicleRequest request){
        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Araç sahibi bulunamadı."));

        String normalizedPlate = request.plate().replace(" ","").toUpperCase(Locale.ROOT);

        if (vehicleRepository.existsByPlate(normalizedPlate)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu plaka zaten kayıtlı.");
        }

        if (request.modelYear() > Year.now().getValue()+1){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Model yılı gelecek yıldan büyük olamaz.");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setOwner(owner);
        vehicle.setPlate(normalizedPlate);
        vehicle.setBrand(request.brand().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setModelYear(request.modelYear());
        vehicle.setFuelType(request.fuelType());
        vehicle.setEngineInfo(request.engineInfo() == null ? null : request.engineInfo().trim());
        vehicle.setCurrentMileage(request.currentMileage());

        try {
            Vehicle savedVehicle = vehicleRepository.saveAndFlush(vehicle);
            return toResponse(savedVehicle);
        }catch (DataIntegrityViolationException exception){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Araç kaydı mevcut verilerle çakışıyor.", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesByOwner(Long ownerId){
        if (ownerId <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Araç sahibi kimliği pozitif olmalıdır.");
        }

        if (!userRepository.existsById(ownerId)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Araç sahibi bulunamadı.");
        }

        return  vehicleRepository
                .findAllByOwner_IdAndArchivedFalseOrderByIdDesc(ownerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id, Long ownerId){
        Vehicle vehicle = findActiveVehicle(id, ownerId);
        return toResponse(vehicle);
    }

    @Transactional
    public VehicleResponse updateVehicle(Long id, Long ownerId, UpdateVehicleRequest request){
        Vehicle vehicle = findActiveVehicle(id, ownerId);
        String normalizedPlate = request.plate()
                .replace(" ", "")
                .toUpperCase(Locale.ROOT);

        if (vehicleRepository.existsByPlateAndIdNot(normalizedPlate, id)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu plaka başka bir araçta kayıtlı.");
        }

        if (request.modelYear() > Year.now().getValue()+1){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Model yılı gelecek yıldan büyük olamaz.");
        }

        if (request.currentMileage() < vehicle.getCurrentMileage()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kilometre mevcut değerden küçük olamaz.");
        }

        vehicle.setPlate(normalizedPlate);
        vehicle.setBrand(request.brand().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setModelYear(request.modelYear());
        vehicle.setFuelType(request.fuelType());
        vehicle.setEngineInfo(request.engineInfo() == null ? null : request.engineInfo().trim());
        vehicle.setCurrentMileage(request.currentMileage());

        try {
            Vehicle updatedVehicle = vehicleRepository.saveAndFlush(vehicle);
            return toResponse(updatedVehicle);
        }catch (DataIntegrityViolationException exception){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Araç güncellemesi mevcut verilerle çakışıyor.", exception);
        }
    }

    private Vehicle findActiveVehicle(Long id, Long ownerId){
        if (id <= 0 ||ownerId <= 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Araç ve kullanıcı kimlikleri pozitif olmalıdır.");
        }

        return vehicleRepository
                .findByIdAndOwner_IdAndArchivedFalse(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bu kullanıcıya ait aktif araç bulunamadı."));
    }


    private VehicleResponse toResponse(Vehicle vehicle){
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getOwner().getId(),
                vehicle.getPlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getModelYear(),
                vehicle.getFuelType(),
                vehicle.getEngineInfo(),
                vehicle.getCurrentMileage()
        );
    }
}
