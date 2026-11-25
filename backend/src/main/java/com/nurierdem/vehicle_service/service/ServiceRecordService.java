package com.nurierdem.vehicle_service.service;

import com.nurierdem.vehicle_service.dto.CreateServiceRecordRequest;
import com.nurierdem.vehicle_service.dto.ServiceRecordResponse;
import com.nurierdem.vehicle_service.entity.*;
import com.nurierdem.vehicle_service.enums.AppointmentStatus;
import com.nurierdem.vehicle_service.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class ServiceRecordService {
    private static final List<AppointmentStatus> HISTORY_STATUSES = List.of(
            AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED);

    private final ServiceRecordRepository serviceRecordRepository;
    private final VehicleRepository vehicleRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public ServiceRecordService(
            ServiceRecordRepository serviceRecordRepository,
            VehicleRepository vehicleRepository,
            AppointmentRepository appointmentRepository,
            UserRepository userRepository
    ) {
        this.serviceRecordRepository = serviceRecordRepository;
        this.vehicleRepository = vehicleRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ServiceRecordResponse createServiceRecord(CreateServiceRecordRequest request) {
        return createRecord(request, true);
    }

    @Transactional
    public ServiceRecordResponse createServiceRecordForManagement(CreateServiceRecordRequest request) {
        return createRecord(request, false);
    }

    private ServiceRecordResponse createRecord(
            CreateServiceRecordRequest request, boolean requireAssignment) {
        requirePositiveId(request.vehicleId());
        User staff = findUser(request.performedById());
        appointmentRepository.lockSchedule();

        Vehicle vehicle = vehicleRepository.findActiveByIdForUpdate(request.vehicleId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Aktif araç bulunamadı."));
        Appointment appointment = findEligibleAppointment(request, staff, requireAssignment);
        validateMileage(vehicle, request.serviceDate(), request.mileage());

        ServiceRecord record = new ServiceRecord();
        record.setVehicle(vehicle);
        record.setAppointment(appointment);
        record.setPerformedBy(staff);
        record.setServiceType(request.serviceType());
        record.setServiceDate(request.serviceDate());
        record.setMileage(request.mileage());
        record.setReplacedParts(request.replacedParts() == null
                ? null : request.replacedParts().trim());
        record.setDescription(request.description().trim());
        record.setCost(request.cost());

        vehicle.setCurrentMileage(Math.max(vehicle.getCurrentMileage(), request.mileage()));
        if (appointment != null) {
            appointment.setStatus(AppointmentStatus.COMPLETED);
        }

        try {
            return toResponse(serviceRecordRepository.saveAndFlush(record));
        } catch (DataIntegrityViolationException failure) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Servis kaydı mevcut verilerle çakışıyor.", failure);
        }
    }

    @Transactional(readOnly = true)
    public List<ServiceRecordResponse> getAllServiceRecords() {
        return serviceRecordRepository.findAllByOrderByServiceDateDescIdDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceRecordResponse> getServiceHistory(Long vehicleId) {
        findVehicle(vehicleId);
        return getHistory(vehicleId);
    }

    @Transactional(readOnly = true)
    public List<ServiceRecordResponse> getOwnerHistory(Long vehicleId, Long ownerId) {
        requirePositiveId(ownerId);
        Vehicle vehicle = findVehicle(vehicleId);
        if (!vehicle.getOwner().getId().equals(ownerId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Bu kullanıcıya ait araç bulunamadı.");
        }
        return getHistory(vehicleId);
    }

    @Transactional(readOnly = true)
    public List<ServiceRecordResponse> getStaffHistory(Long vehicleId, Long staffId) {
        findUser(staffId);
        findVehicle(vehicleId);
        boolean assigned = appointmentRepository.existsByVehicle_IdAndAssignedStaff_IdAndStatusIn(
                vehicleId, staffId, HISTORY_STATUSES);
        if (!assigned) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Bu araç görev kapsamınızda değil.");
        }
        return getHistory(vehicleId);
    }

    private Appointment findEligibleAppointment(
            CreateServiceRecordRequest request, User staff, boolean requireAssignment) {
        if (request.appointmentId() == null) {
            if (requireAssignment
                    && !appointmentRepository.existsByVehicle_IdAndAssignedStaff_IdAndStatusIn(
                    request.vehicleId(), staff.getId(), List.of(AppointmentStatus.CONFIRMED))) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Bu araçta onaylı bir göreviniz bulunmuyor.");
            }
            return null;
        }

        requirePositiveId(request.appointmentId());
        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Randevu bulunamadı."));
        if (!appointment.getVehicle().getId().equals(request.vehicleId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Randevu ile araç eşleşmiyor.");
        }
        if (appointment.getAssignedStaff() == null
                || !appointment.getAssignedStaff().getId().equals(staff.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Bu randevu size atanmış değil.");
        }
        if (serviceRecordRepository.existsByAppointment_Id(appointment.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Bu randevu için servis kaydı zaten bulunuyor.");
        }
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Servis kaydı için randevu onaylanmış olmalıdır.");
        }
        return appointment;
    }

    private void validateMileage(Vehicle vehicle, LocalDate date, Integer mileage) {
        if (date == null || date.isAfter(LocalDate.now()) || mileage == null || mileage < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "İşlem tarihi veya kilometre geçersiz.");
        }
        if (date.isEqual(LocalDate.now()) && mileage < vehicle.getCurrentMileage()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Bugünkü işlem kilometresi mevcut kilometreden küçük olamaz.");
        }
        boolean earlierHigher = serviceRecordRepository
                .existsByVehicle_IdAndServiceDateBeforeAndMileageGreaterThan(vehicle.getId(), date, mileage);
        boolean laterLower = serviceRecordRepository
                .existsByVehicle_IdAndServiceDateAfterAndMileageLessThan(vehicle.getId(), date, mileage);
        if (earlierHigher || laterLower) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Kilometre, servis geçmişinin tarih sırasıyla çelişiyor.");
        }
    }

    private List<ServiceRecordResponse> getHistory(Long vehicleId) {
        return serviceRecordRepository.findAllByVehicle_IdOrderByServiceDateDescIdDesc(vehicleId)
                .stream().map(this::toResponse).toList();
    }

    private Vehicle findVehicle(Long id) {
        requirePositiveId(id);
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Araç bulunamadı."));
    }

    private User findUser(Long id) {
        requirePositiveId(id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı."));
    }

    private void requirePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kimlik değeri pozitif olmalıdır.");
        }
    }

    private ServiceRecordResponse toResponse(ServiceRecord record) {
        return new ServiceRecordResponse(
                record.getId(), record.getVehicle().getId(), record.getVehicle().getPlate(),
                record.getAppointment() == null ? null : record.getAppointment().getId(),
                record.getPerformedBy().getId(), record.getPerformedBy().getFullName(),
                record.getServiceType(), record.getServiceDate(), record.getMileage(),
                record.getReplacedParts(), record.getDescription(), record.getCost());
    }
}
