package com.nurierdem.vehicle_service.controller;

import com.nurierdem.vehicle_service.dto.CreateVehicleRequest;
import com.nurierdem.vehicle_service.dto.UpdateVehicleRequest;
import com.nurierdem.vehicle_service.dto.VehicleResponse;
import com.nurierdem.vehicle_service.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody CreateVehicleRequest request){
        VehicleResponse response = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<VehicleResponse> getVehicles(@RequestParam("ownerId") Long ownerId){
        return vehicleService.getVehiclesByOwner(ownerId);
    }

    @GetMapping("/{id}")
    public VehicleResponse getVehicleById(@PathVariable("id") Long id, @RequestParam("ownerId") Long ownerId){
        return vehicleService.getVehicleById(id, ownerId);
    }

    @PutMapping("/{id}")
    public VehicleResponse updateVehicle(@PathVariable("id") Long id, @RequestParam("ownerId") Long ownerId, @Valid @RequestBody UpdateVehicleRequest request){
        return vehicleService.updateVehicle(id,ownerId,request);
    }
}
