package com.nurierdem.vehicle_service.service;

import com.nurierdem.vehicle_service.dto.*;
import com.nurierdem.vehicle_service.entity.Appointment;
import com.nurierdem.vehicle_service.entity.User;
import com.nurierdem.vehicle_service.entity.Vehicle;
import com.nurierdem.vehicle_service.enums.AppointmentStatus;
import com.nurierdem.vehicle_service.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {
    private static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(
                    AppointmentStatus.PENDING,
                    AppointmentStatus.CONFIRMED
            );

    private final AppointmentRepository appointmentRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final ServiceRecordRepository serviceRecordRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            VehicleRepository vehicleRepository,
            UserRepository userRepository,
            ServiceRecordRepository serviceRecordRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.serviceRecordRepository = serviceRecordRepository;
    }

    @Transactional
    public AppointmentResponse createAppointment(
            CreateAppointmentRequest request
    ) {
        requirePositiveId(request.ownerId());
        requirePositiveId(request.vehicleId());

        Vehicle vehicle = vehicleRepository
                .findByIdAndOwner_IdAndArchivedFalse(
                        request.vehicleId(),
                        request.ownerId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bu kullanıcıya ait aktif araç bulunamadı."
                ));

        appointmentRepository.lockSchedule();

        LocalDateTime dateTime = request.appointmentDateTime();
        validateAppointmentTime(dateTime);

        boolean occupied =
                appointmentRepository.existsByAppointmentDateTimeAndStatusIn(
                        dateTime,
                        ACTIVE_STATUSES
                );

        if (occupied) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Bu tarih ve saat için aktif randevu bulunuyor."
            );
        }

        Appointment appointment = new Appointment();
        appointment.setVehicle(vehicle);
        appointment.setAppointmentDateTime(dateTime);
        appointment.setRequestDescription(
                request.requestDescription().trim()
        );
        appointment.setStatus(AppointmentStatus.PENDING);

        Appointment saved =
                appointmentRepository.saveAndFlush(appointment);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getOwnerAppointments(
            Long ownerId,
            AppointmentStatus status
    ) {
        findUser(ownerId);

        List<Appointment> appointments = status == null
                ? appointmentRepository
                .findAllByVehicle_Owner_IdOrderByAppointmentDateTimeDescIdDesc(
                        ownerId
                )
                : appointmentRepository
                .findAllByVehicle_Owner_IdAndStatusOrderByAppointmentDateTimeDescIdDesc(
                        ownerId,
                        status
                );

        return appointments.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAssignedAppointments(
            Long staffId,
            AppointmentStatus status
    ) {
        findUser(staffId);

        List<Appointment> appointments = status == null
                ? appointmentRepository
                .findAllByAssignedStaff_IdOrderByAppointmentDateTimeDescIdDesc(
                        staffId
                )
                : appointmentRepository
                .findAllByAssignedStaff_IdAndStatusOrderByAppointmentDateTimeDescIdDesc(
                        staffId,
                        status
                );

        return appointments.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getServiceAppointments(
            AppointmentStatus status
    ) {
        List<Appointment> appointments = status == null
                ? appointmentRepository
                .findAllByOrderByAppointmentDateTimeDescIdDesc()
                : appointmentRepository
                .findAllByStatusOrderByAppointmentDateTimeDescIdDesc(
                        status
                );

        return appointments.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getOwnerAppointment(
            Long id,
            Long ownerId
    ) {
        requirePositiveId(ownerId);

        Appointment appointment = findAppointment(id);

        if (!appointment.getVehicle().getOwner().getId().equals(ownerId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Bu kullanıcıya ait randevu bulunamadı."
            );
        }

        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse assignStaff(
            Long id,
            AssignAppointmentStaffRequest request
    ) {
        appointmentRepository.lockSchedule();

        Appointment appointment = findAppointment(id);

        if (!ACTIVE_STATUSES.contains(appointment.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Kapanmış randevunun personeli değiştirilemez."
            );
        }

        User staff = findUser(request.staffId());
        appointment.setAssignedStaff(staff);

        return toResponse(
                appointmentRepository.saveAndFlush(appointment)
        );
    }

    @Transactional
    public AppointmentResponse updateStatus(
            Long id,
            UpdateAppointmentStatusRequest request
    ) {
        appointmentRepository.lockSchedule();

        Appointment appointment = findAppointment(id);
        AppointmentStatus nextStatus = request.status();

        if (nextStatus == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Randevu durumu zorunludur."
            );
        }

        if (appointment.getStatus() == nextStatus) {
            return toResponse(appointment);
        }

        boolean allowed = switch (appointment.getStatus()) {
            case PENDING ->
                    nextStatus == AppointmentStatus.CONFIRMED
                            || nextStatus == AppointmentStatus.CANCELLED;

            case CONFIRMED ->
                    nextStatus == AppointmentStatus.CANCELLED
                            || nextStatus == AppointmentStatus.COMPLETED;

            case CANCELLED, COMPLETED -> false;
        };

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Bu randevu durum değişikliğine izin verilmiyor."
            );
        }

        if (nextStatus == AppointmentStatus.CONFIRMED
                && appointment.getAssignedStaff() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Onaylamadan önce personel atayın."
            );
        }

        if (nextStatus == AppointmentStatus.COMPLETED
                && !serviceRecordRepository.existsByAppointment_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tamamlamadan önce servis kaydı oluşturun."
            );
        }

        appointment.setStatus(nextStatus);

        return toResponse(
                appointmentRepository.saveAndFlush(appointment)
        );
    }

    private Appointment findAppointment(Long id) {
        requirePositiveId(id);

        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Randevu bulunamadı."
                ));
    }

    private User findUser(Long id) {
        requirePositiveId(id);

        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Kullanıcı bulunamadı."
                ));
    }

    private void requirePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Kimlik değeri pozitif olmalıdır."
            );
        }
    }

    private void validateAppointmentTime(LocalDateTime dateTime) {
        if (dateTime == null
                || !dateTime.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Randevu gelecekte olmalıdır."
            );
        }

        if (dateTime.getMinute() % 30 != 0
                || dateTime.getSecond() != 0
                || dateTime.getNano() != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Randevuyu 30 dakikalık dilimlerden seçin: 10:00, 10:30 gibi."
            );
        }
    }

    private AppointmentResponse toResponse(
            Appointment appointment
    ) {
        User staff = appointment.getAssignedStaff();

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getVehicle().getId(),
                appointment.getVehicle().getPlate(),
                appointment.getAppointmentDateTime(),
                appointment.getRequestDescription(),
                appointment.getStatus(),
                staff == null ? null : staff.getId(),
                staff == null ? null : staff.getFullName()
        );
    }
}