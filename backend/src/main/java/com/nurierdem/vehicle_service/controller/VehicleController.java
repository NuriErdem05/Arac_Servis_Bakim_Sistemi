package com.nurierdem.vehicle_service.controller;

import com.nurierdem.vehicle_service.dto.CreateVehicleRequest;
import com.nurierdem.vehicle_service.dto.MaintenancePlanResponse;
import com.nurierdem.vehicle_service.dto.UpdateVehicleRequest;
import com.nurierdem.vehicle_service.dto.VehicleResponse;
import com.nurierdem.vehicle_service.security.AccountAccess;
import com.nurierdem.vehicle_service.service.VehicleService;
import com.nurierdem.vehicle_service.service.MaintenancePlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final AccountAccess access;
    private final VehicleService vehicleService;
    private final MaintenancePlanService maintenancePlanService;

    public VehicleController(VehicleService vehicleService, MaintenancePlanService maintenancePlanService, AccountAccess access) {
        this.access = access;
        this.vehicleService = vehicleService;
        this.maintenancePlanService = maintenancePlanService;
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody CreateVehicleRequest request){
        access.requireSelfOrAdmin(request.ownerId());
        VehicleResponse response = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<VehicleResponse> getVehicles(@RequestParam("ownerId") Long ownerId){
        access.requireSelf(ownerId);
        return vehicleService.getVehiclesByOwner(ownerId);
    }

    @GetMapping("/service")
    public List<VehicleResponse> getServiceVehicles() {
        return vehicleService.getServiceVehicles();
    }

    @GetMapping("/service/{id}")
    public VehicleResponse getServiceVehicle(@PathVariable("id") Long id) {
        return vehicleService.getServiceVehicle(id);
    }

    @GetMapping("/service/{id}/maintenance-plan")
    public MaintenancePlanResponse getServiceMaintenancePlan(@PathVariable("id") Long id) {
        return maintenancePlanService.getServicePlan(id);
    }

    @PutMapping("/service/{id}")
    public VehicleResponse updateServiceVehicle(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateVehicleRequest request) {
        return vehicleService.updateServiceVehicle(id, request);
    }

    @GetMapping("/{id}")
    public VehicleResponse getVehicleById(@PathVariable("id") Long id, @RequestParam("ownerId") Long ownerId){
        access.requireSelf(ownerId);
        return vehicleService.getVehicleById(id, ownerId);
    }

    @GetMapping("/{id}/maintenance-plan")
    public MaintenancePlanResponse getOwnerMaintenancePlan(
            @PathVariable("id") Long id, @RequestParam("ownerId") Long ownerId) {
        access.requireSelf(ownerId);
        return maintenancePlanService.getOwnerPlan(id, ownerId);
    }

    @PutMapping("/{id}")
    public VehicleResponse updateVehicle(@PathVariable("id") Long id, @RequestParam("ownerId") Long ownerId, @Valid @RequestBody UpdateVehicleRequest request){
        access.requireSelf(ownerId);
        return vehicleService.updateVehicle(id,ownerId,request);
    }
}
