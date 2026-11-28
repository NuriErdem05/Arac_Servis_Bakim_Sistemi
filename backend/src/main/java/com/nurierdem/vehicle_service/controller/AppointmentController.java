package com.nurierdem.vehicle_service.controller;

import com.nurierdem.vehicle_service.dto.*;
import com.nurierdem.vehicle_service.enums.AppointmentStatus;
import com.nurierdem.vehicle_service.security.AccountAccess;
import com.nurierdem.vehicle_service.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AccountAccess access;
    private final AppointmentService appointmentService;

    public AppointmentController(
            AppointmentService appointmentService, AccountAccess access
    ) {
        this.access = access;
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(
            @Valid @RequestBody CreateAppointmentRequest request
    ) {
        access.requireSelfOrAdmin(request.ownerId());
        AppointmentResponse response =
                appointmentService.createAppointment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public List<AppointmentResponse> getOwnerAppointments(
            @RequestParam("ownerId") Long ownerId,
            @RequestParam(value = "status", required = false)
            AppointmentStatus status
    ) {
        access.requireSelf(ownerId);
        return appointmentService.getOwnerAppointments(
                ownerId,
                status
        );
    }

    @GetMapping("/assigned")
    public List<AppointmentResponse> getAssignedAppointments(
            @RequestParam("staffId") Long staffId,
            @RequestParam(value = "status", required = false)
            AppointmentStatus status
    ) {
        access.requireSelf(staffId);
        return appointmentService.getAssignedAppointments(
                staffId,
                status
        );
    }

    @GetMapping("/service")
    public List<AppointmentResponse> getServiceAppointments(
            @RequestParam(value = "status", required = false)
            AppointmentStatus status
    ) {
        return appointmentService.getServiceAppointments(status);
    }

    @GetMapping("/service/{id}")
    public AppointmentResponse getServiceAppointment(@PathVariable("id") Long id) {
        return appointmentService.getServiceAppointment(id);
    }

    @GetMapping("/{id}")
    public AppointmentResponse getOwnerAppointment(
            @PathVariable("id") Long id,
            @RequestParam("ownerId") Long ownerId
    ) {
        access.requireSelf(ownerId);
        return appointmentService.getOwnerAppointment(
                id,
                ownerId
        );
    }

    @PutMapping("/{id}/staff")
    public AppointmentResponse assignStaff(
            @PathVariable("id") Long id,
            @Valid @RequestBody AssignAppointmentStaffRequest request
    ) {
        return appointmentService.assignStaff(id, request);
    }

    @PutMapping("/{id}/status")
    public AppointmentResponse updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAppointmentStatusRequest request
    ) {
        return appointmentService.updateStatus(id, request);
    }
}
