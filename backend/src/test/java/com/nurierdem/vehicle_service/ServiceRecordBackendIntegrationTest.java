package com.nurierdem.vehicle_service;

import com.nurierdem.vehicle_service.dto.*;
import com.nurierdem.vehicle_service.entity.*;
import com.nurierdem.vehicle_service.enums.*;
import com.nurierdem.vehicle_service.repository.*;
import com.nurierdem.vehicle_service.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfSystemProperty(named = "test.db.url", matches = "jdbc:postgresql://127\\.0\\.0\\.1:.*")
@Import(BusinessTestAccountScope.class)
@SpringBootTest(properties = {
        "APP_JWT_SECRET=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.datasource.username=backend_test",
        "spring.datasource.password=",
        "spring.sql.init.mode=never"
})
class ServiceRecordBackendIntegrationTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getProperty("test.db.url"));
    }

    @Autowired ServiceRecordService service;
    @Autowired AppointmentService appointmentService;
    @Autowired VehicleService vehicleService;
    @Autowired ServiceRecordRepository records;
    @Autowired AppointmentRepository appointments;
    @Autowired VehicleRepository vehicles;
    @Autowired UserRepository users;
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;

    MockMvc mvc;
    User owner;
    User otherOwner;
    User staff;
    User otherStaff;
    Vehicle vehicle;
    Vehicle otherVehicle;
    Long appointmentId;

    @BeforeEach
    void setup() {
        records.deleteAll();
        appointments.deleteAll();
        vehicles.deleteAll();
        users.deleteAll();
        owner = saveUser("Sahip", "owner@example.test");
        otherOwner = saveUser("Başka sahip", "other@example.test");
        staff = saveUser("Personel", "staff@example.test");
        otherStaff = saveUser("Başka personel", "otherstaff@example.test");
        vehicle = saveVehicle(owner, "58ABC123");
        otherVehicle = saveVehicle(otherOwner, "58DBC123");
        LocalDateTime time = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        appointmentId = appointmentService.createAppointment(new CreateAppointmentRequest(
                owner.getId(), vehicle.getId(), time, "Bakım")).id();
        appointmentService.assignStaff(appointmentId, new AssignAppointmentStaffRequest(staff.getId()));
        appointmentService.updateStatus(appointmentId, new UpdateAppointmentStatusRequest(AppointmentStatus.CONFIRMED));
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void createCompletesAppointmentUpdatesMileageAndReturnsExactHistory() throws Exception {
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(json(
                        vehicle.getId(), appointmentId, staff.getId(), LocalDate.now(), 46000, "1250.75")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plate").value("58ABC123"))
                .andExpect(jsonPath("$.performedByName").value("Personel"))
                .andExpect(jsonPath("$.description").value("Yağ bakımı"))
                .andExpect(jsonPath("$.replacedParts").value("Yağ filtresi"));
        assertThat(vehicles.findById(vehicle.getId()).orElseThrow().getCurrentMileage()).isEqualTo(46000);
        assertThat(appointments.findById(appointmentId).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(records.findAll().getFirst().getCost()).isEqualByComparingTo("1250.75");
        mvc.perform(get("/api/vehicles/{id}", vehicle.getId()).param("ownerId", owner.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentMileage").value(46000));
        mvc.perform(get("/api/vehicles/{id}/service-records", vehicle.getId()).param("ownerId", owner.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/vehicles/{id}/service-records/assigned", vehicle.getId()).param("staffId", staff.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void maintenancePlanUsesLatestMaintenanceAndRespectsVehicleScope() throws Exception {
        LocalDate lastMaintenance = LocalDate.now().minusMonths(4);
        service.createServiceRecordForManagement(new CreateServiceRecordRequest(
                vehicle.getId(), null, staff.getId(), ServiceType.REPAIR,
                lastMaintenance.plusDays(12), 47500, null, "Fren onarımı", BigDecimal.ZERO));
        mvc.perform(get("/api/vehicles/service/{id}/maintenance-plan", vehicle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalMonths").value(12))
                .andExpect(jsonPath("$.intervalKilometers").value(15000))
                .andExpect(jsonPath("$.schedule").doesNotExist());

        service.createServiceRecordForManagement(request(null, lastMaintenance.minusMonths(2), 42000));
        service.createServiceRecordForManagement(request(null, lastMaintenance, 45000));
        service.createServiceRecordForManagement(request(null, lastMaintenance, 44000));
        mvc.perform(get("/api/vehicles/{id}/maintenance-plan", vehicle.getId())
                        .param("ownerId", owner.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedule.lastMaintenanceDate").value(lastMaintenance.toString()))
                .andExpect(jsonPath("$.schedule.lastMaintenanceMileage").value(45000))
                .andExpect(jsonPath("$.schedule.nextMaintenanceDate").value(lastMaintenance.plusMonths(12).toString()))
                .andExpect(jsonPath("$.schedule.nextMaintenanceMileage").value(60000))
                .andExpect(jsonPath("$.schedule.kilometersUntilMaintenance").value(12500))
                .andExpect(jsonPath("$.schedule.status").value("ON_TRACK"));
        mvc.perform(get("/api/vehicles/{id}/maintenance-plan", vehicle.getId())
                        .param("ownerId", otherOwner.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void maintenancePlanUpdatesAsMileageCrossesReminderAndDeadline() throws Exception {
        service.createServiceRecordForManagement(request(null, LocalDate.now().minusDays(1), 45000));
        String path = "/api/vehicles/service/{id}/maintenance-plan";
        vehicle.setCurrentMileage(59000);
        vehicles.saveAndFlush(vehicle);
        mvc.perform(get(path, vehicle.getId()))
                .andExpect(jsonPath("$.schedule.status").value("APPROACHING"));
        vehicle.setCurrentMileage(60000);
        vehicles.saveAndFlush(vehicle);
        mvc.perform(get(path, vehicle.getId()))
                .andExpect(jsonPath("$.schedule.status").value("DUE"))
                .andExpect(jsonPath("$.schedule.kilometersUntilMaintenance").value(0));
        vehicle.setCurrentMileage(60001);
        vehicles.saveAndFlush(vehicle);
        mvc.perform(get(path, vehicle.getId()))
                .andExpect(jsonPath("$.schedule.status").value("OVERDUE"))
                .andExpect(jsonPath("$.schedule.kilometersUntilMaintenance").value(-1));
    }

    @Test
    void overdueDateTakesPriorityWhenMileageIsBelowTheThreshold() throws Exception {
        service.createServiceRecordForManagement(request(null, LocalDate.now().minusMonths(13), 45000));
        mvc.perform(get("/api/vehicles/service/{id}/maintenance-plan", vehicle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedule.status").value("OVERDUE"))
                .andExpect(jsonPath("$.schedule.kilometersUntilMaintenance").value(15000));
    }

    @Test
    void duplicateAppointmentRecordIs409WithoutChangingMileage() throws Exception {
        service.createServiceRecord(request(appointmentId, LocalDate.now(), 46000));
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(json(
                        vehicle.getId(), appointmentId, staff.getId(), LocalDate.now(), 47000, "100")))
                .andExpect(status().isConflict());
        assertThat(records.count()).isEqualTo(1);
        assertThat(vehicles.findById(vehicle.getId()).orElseThrow().getCurrentMileage()).isEqualTo(46000);
    }

    @Test
    void vehicleMismatchWrongStaffAndUnconfirmedAppointmentCannotWrite() throws Exception {
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(json(
                        otherVehicle.getId(), appointmentId, staff.getId(), LocalDate.now(), 46000, "100")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(json(
                        vehicle.getId(), appointmentId, otherStaff.getId(), LocalDate.now(), 46000, "100")))
                .andExpect(status().isForbidden());
        Appointment appointment = appointments.findById(appointmentId).orElseThrow();
        appointment.setStatus(AppointmentStatus.PENDING);
        appointments.saveAndFlush(appointment);
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(json(
                        vehicle.getId(), appointmentId, staff.getId(), LocalDate.now(), 46000, "100")))
                .andExpect(status().isConflict());
        assertThat(records.count()).isZero();
        assertThat(vehicles.findById(vehicle.getId()).orElseThrow().getCurrentMileage()).isEqualTo(45000);
    }

    @Test
    void ownerAndStaffHistoryRespectRecordScopeAndEmptyHistoryIsAList() throws Exception {
        mvc.perform(get("/api/vehicles/{id}/service-records", vehicle.getId()).param("ownerId", owner.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/vehicles/{id}/service-records", vehicle.getId()).param("ownerId", otherOwner.getId().toString()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/vehicles/{id}/service-records/assigned", vehicle.getId()).param("staffId", otherStaff.getId().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/vehicles/999999/service-records").param("ownerId", owner.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void historicalRecordsKeepCurrentMileageAndSortByServiceDate() {
        service.createServiceRecord(request(null, LocalDate.now().minusDays(10), 40000));
        service.createServiceRecord(request(null, LocalDate.now().minusDays(5), 42000));
        List<ServiceRecordResponse> history = service.getOwnerHistory(vehicle.getId(), owner.getId());
        assertThat(history).extracting(ServiceRecordResponse::mileage).containsExactly(42000, 40000);
        assertThat(history).allMatch(item -> item.appointmentId() == null);
        assertThat(vehicles.findById(vehicle.getId()).orElseThrow().getCurrentMileage()).isEqualTo(45000);
        assertThat(appointments.findById(appointmentId).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void historicalMileageCannotContradictEarlierOrLaterRecord() throws Exception {
        service.createServiceRecord(request(null, LocalDate.now().minusDays(10), 40000));
        service.createServiceRecord(request(null, LocalDate.now().minusDays(1), 44000));
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(json(
                        vehicle.getId(), null, staff.getId(), LocalDate.now().minusDays(5), 39000, "100")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(json(
                        vehicle.getId(), null, staff.getId(), LocalDate.now().minusDays(5), 45000, "100")))
                .andExpect(status().isBadRequest());
        assertThat(records.count()).isEqualTo(2);
    }

    @Test
    void validationAndTodaysMileageDecreaseLeaveDatabaseUnchanged() throws Exception {
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.vehicleId").exists());
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(json(
                        vehicle.getId(), appointmentId, staff.getId(), LocalDate.now(), 44000, "100")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(json(
                        vehicle.getId(), appointmentId, staff.getId(), LocalDate.now().plusDays(1), 46000, "100")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.serviceDate").exists());
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(json(
                        vehicle.getId(), appointmentId, staff.getId(), LocalDate.now(), 46000, "-1")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.cost").exists());
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(json(
                        vehicle.getId(), appointmentId, staff.getId(), LocalDate.now(), -1, "100")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.mileage").exists());
        assertThat(records.count()).isZero();
        assertThat(appointments.findById(appointmentId).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void databaseFailureRollsBackInsertedRecordMileageAndAppointment() throws Exception {
        jdbc.execute("alter table vehicles add constraint test_reject_service_mileage check (current_mileage <> 46000)");
        try {
            mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(json(
                            vehicle.getId(), appointmentId, staff.getId(), LocalDate.now(), 46000, "100")))
                    .andExpect(status().isConflict());
        } finally {
            jdbc.execute("alter table vehicles drop constraint test_reject_service_mileage");
        }
        assertThat(records.count()).isZero();
        assertThat(vehicles.findById(vehicle.getId()).orElseThrow().getCurrentMileage()).isEqualTo(45000);
        assertThat(appointments.findById(appointmentId).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void simultaneousDuplicateRecordCreatesOnlyOne() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Integer> create = () -> {
                start.await();
                try {
                    service.createServiceRecord(request(appointmentId, LocalDate.now(), 46000));
                    return 201;
                } catch (ResponseStatusException error) {
                    return error.getStatusCode().value();
                }
            };
            Future<Integer> one = executor.submit(create);
            Future<Integer> two = executor.submit(create);
            start.countDown();
            assertThat(List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(records.count()).isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }

    @Test
    void simultaneousVehicleEditCannotOverwriteServiceMileageWithLowerValue() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> record = executor.submit(() -> {
                start.await();
                service.createServiceRecord(request(appointmentId, LocalDate.now(), 46000));
                return 201;
            });
            Future<Integer> edit = executor.submit(() -> {
                start.await();
                try {
                    vehicleService.updateVehicle(vehicle.getId(), owner.getId(), new UpdateVehicleRequest(
                            "58ABC123", "Renault", "Clio Touch", 2022, FuelType.GASOLINE, "1.0", 45500));
                    return 200;
                } catch (ResponseStatusException error) { return error.getStatusCode().value(); }
            });
            start.countDown();
            assertThat(record.get(15, TimeUnit.SECONDS)).isEqualTo(201);
            assertThat(edit.get(15, TimeUnit.SECONDS)).isIn(200, 400);
            assertThat(vehicles.findById(vehicle.getId()).orElseThrow().getCurrentMileage()).isEqualTo(46000);
        } finally { executor.shutdownNow(); }
    }

    @Test
    void archivedVehicleCannotReceiveNewRecordButHistoryRemainsReadable() throws Exception {
        service.createServiceRecord(request(null, LocalDate.now().minusDays(1), 44000));
        Vehicle saved = vehicles.findById(vehicle.getId()).orElseThrow();
        saved.setArchived(true);
        vehicles.saveAndFlush(saved);
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(json(
                        vehicle.getId(), appointmentId, staff.getId(), LocalDate.now(), 46000, "100")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/vehicles/{id}/service-records", vehicle.getId()).param("ownerId", owner.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unlinkedRecordStillRequiresConfirmedAssignmentAndDoesNotCompleteAppointment() throws Exception {
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(json(
                        vehicle.getId(), null, otherStaff.getId(), LocalDate.now(), 46000, "100")))
                .andExpect(status().isForbidden());
        Appointment saved = appointments.findById(appointmentId).orElseThrow();
        saved.setStatus(AppointmentStatus.PENDING);
        appointments.saveAndFlush(saved);
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(json(
                        vehicle.getId(), null, staff.getId(), LocalDate.now(), 46000, "100")))
                .andExpect(status().isForbidden());
        appointmentService.updateStatus(appointmentId, new UpdateAppointmentStatusRequest(AppointmentStatus.CONFIRMED));
        ServiceRecordResponse response = service.createServiceRecord(request(null, LocalDate.now(), 46000));
        assertThat(response.appointmentId()).isNull();
        assertThat(appointments.findById(appointmentId).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    private CreateServiceRecordRequest request(Long appointment, LocalDate date, Integer mileage) {
        return new CreateServiceRecordRequest(vehicle.getId(), appointment, staff.getId(), ServiceType.MAINTENANCE,
                date, mileage, "  Yağ filtresi  ", "  Yağ bakımı  ", new BigDecimal("1250.75"));
    }

    private String json(Long vehicleId, Long appointment, Long staffId, LocalDate date, Integer mileage, String cost) {
        return "{\"vehicleId\":" + vehicleId + ",\"appointmentId\":" + appointment
                + ",\"performedById\":" + staffId + ",\"serviceType\":\"MAINTENANCE\",\"serviceDate\":\""
                + date + "\",\"mileage\":" + mileage + ",\"replacedParts\":\"  Yağ filtresi  \","
                + "\"description\":\"  Yağ bakımı  \",\"cost\":" + cost + "}";
    }

    private User saveUser(String name, String email) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setRole(email.contains("staff") ? UserRole.STAFF : UserRole.USER);
        return users.saveAndFlush(user);
    }

    private Vehicle saveVehicle(User user, String plate) {
        Vehicle item = new Vehicle();
        item.setOwner(user);
        item.setPlate(plate);
        item.setBrand("Renault");
        item.setModel("Clio");
        item.setModelYear(2022);
        item.setFuelType(FuelType.GASOLINE);
        item.setCurrentMileage(45000);
        return vehicles.saveAndFlush(item);
    }
}
