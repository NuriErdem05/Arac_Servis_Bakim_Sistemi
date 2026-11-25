package com.nurierdem.vehicle_service.controller;

import com.nurierdem.vehicle_service.dto.CreateServiceRecordRequest;
import com.nurierdem.vehicle_service.dto.ServiceRecordResponse;
import com.nurierdem.vehicle_service.service.ServiceRecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ServiceRecordController {
    private final ServiceRecordService serviceRecordService;

    public ServiceRecordController(ServiceRecordService serviceRecordService) {
        this.serviceRecordService = serviceRecordService;
    }

    @PostMapping("/service-records")
    public ResponseEntity<ServiceRecordResponse> create(
            @Valid @RequestBody CreateServiceRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceRecordService.createServiceRecord(request));
    }

    @GetMapping("/service-records/service")
    public List<ServiceRecordResponse> getServiceRecords() {
        return serviceRecordService.getAllServiceRecords();
    }

    @PostMapping("/service-records/service")
    public ResponseEntity<ServiceRecordResponse> createForManagement(
            @Valid @RequestBody CreateServiceRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceRecordService.createServiceRecordForManagement(request));
    }

    @GetMapping("/vehicles/{vehicleId}/service-records/service")
    public List<ServiceRecordResponse> getServiceHistory(
            @PathVariable("vehicleId") Long vehicleId) {
        return serviceRecordService.getServiceHistory(vehicleId);
    }

    @GetMapping("/vehicles/{vehicleId}/service-records")
    public List<ServiceRecordResponse> getOwnerHistory(
            @PathVariable("vehicleId") Long vehicleId,
            @RequestParam("ownerId") Long ownerId) {
        return serviceRecordService.getOwnerHistory(vehicleId, ownerId);
    }

    @GetMapping("/vehicles/{vehicleId}/service-records/assigned")
    public List<ServiceRecordResponse> getStaffHistory(
            @PathVariable("vehicleId") Long vehicleId,
            @RequestParam("staffId") Long staffId) {
        return serviceRecordService.getStaffHistory(vehicleId, staffId);
    }
}
