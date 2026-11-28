package com.nurierdem.vehicle_service;

import com.nurierdem.vehicle_service.dto.*;
import com.nurierdem.vehicle_service.entity.*;
import com.nurierdem.vehicle_service.enums.*;
import com.nurierdem.vehicle_service.repository.*;
import com.nurierdem.vehicle_service.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class AppointmentBackendIntegrationTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getProperty("test.db.url"));
    }

    @Autowired AppointmentService service;
    @Autowired AppointmentRepository appointments;
    @Autowired ServiceRecordRepository records;
    @Autowired VehicleRepository vehicles;
    @Autowired UserRepository users;
    @Autowired WebApplicationContext context;

    MockMvc mvc;
    User owner;
    User otherOwner;
    User staff;
    Vehicle vehicle;
    Vehicle otherVehicle;
    LocalDateTime time;

    @BeforeEach
    void setup() {
        records.deleteAll();
        appointments.deleteAll();
        vehicles.deleteAll();
        users.deleteAll();
        owner = saveUser("Araç sahibi", "owner@example.test");
        otherOwner = saveUser("Diğer sahip", "other@example.test");
        staff = saveUser("Test personeli", "staff@example.test");
        vehicle = saveVehicle(owner, "58ABC123");
        otherVehicle = saveVehicle(otherOwner, "58DBC123");
        time = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void createListFilterAndOwnerDetailUseRealDatabase() throws Exception {
        mvc.perform(post("/api/appointments").contentType(APPLICATION_JSON)
                        .content(createJson(owner.getId(), vehicle.getId(), time)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plate").value("58ABC123"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requestDescription").value("Yağ bakımı"));
        Long id = appointments.findAll().getFirst().getId();
        mvc.perform(get("/api/appointments").param("ownerId", owner.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/appointments").param("ownerId", owner.getId().toString())
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/appointments/{id}", id).param("ownerId", otherOwner.getId().toString()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/appointments/{id}", id).param("ownerId", owner.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void invalidDtoPastDateWrongOwnerAndUnalignedTimeAreRejected() throws Exception {
        mvc.perform(post("/api/appointments").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.ownerId").exists());
        mvc.perform(post("/api/appointments").contentType(APPLICATION_JSON)
                        .content(createJson(owner.getId(), vehicle.getId(), time.minusYears(10))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.appointmentDateTime").exists());
        mvc.perform(post("/api/appointments").contentType(APPLICATION_JSON)
                        .content(createJson(otherOwner.getId(), vehicle.getId(), time)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/appointments").contentType(APPLICATION_JSON)
                        .content(createJson(owner.getId(), vehicle.getId(), time.plusMinutes(15))))
                .andExpect(status().isBadRequest());
        assertThat(appointments.count()).isZero();
    }

    @Test
    void activeSlotConflictIs409AndCancellationFreesSlot() throws Exception {
        AppointmentResponse first = create(vehicle);
        mvc.perform(post("/api/appointments").contentType(APPLICATION_JSON)
                        .content(createJson(otherOwner.getId(), otherVehicle.getId(), time)))
                .andExpect(status().isConflict());
        service.updateStatus(first.id(), new UpdateAppointmentStatusRequest(AppointmentStatus.CANCELLED));
        AppointmentResponse second = create(otherVehicle);
        assertThat(second.status()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(appointments.count()).isEqualTo(2);
    }

    @Test
    void assignmentAndConfirmationUpdateAssignedAndServiceLists() throws Exception {
        Long id = create(vehicle).id();
        mvc.perform(put("/api/appointments/{id}/status", id).contentType(APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/appointments/{id}/staff", id).contentType(APPLICATION_JSON)
                        .content("{\"staffId\":" + staff.getId() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assignedStaffName").value("Test personeli"));
        mvc.perform(put("/api/appointments/{id}/status", id).contentType(APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(get("/api/appointments/assigned").param("staffId", staff.getId().toString())
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/appointments/service").param("status", "CONFIRMED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void cancelledRecordCannotReopenOrBeReassigned() throws Exception {
        Long id = create(vehicle).id();
        service.updateStatus(id, new UpdateAppointmentStatusRequest(AppointmentStatus.CANCELLED));
        mvc.perform(put("/api/appointments/{id}/status", id).contentType(APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/appointments/{id}/staff", id).contentType(APPLICATION_JSON)
                        .content("{\"staffId\":" + staff.getId() + "}"))
                .andExpect(status().isConflict());
        assertThat(appointments.findById(id).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void completionRequiresConfirmedAppointmentAndServiceRecord() throws Exception {
        Long id = create(vehicle).id();
        service.assignStaff(id, new AssignAppointmentStaffRequest(staff.getId()));
        mvc.perform(put("/api/appointments/{id}/status", id).contentType(APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict());
        service.updateStatus(id, new UpdateAppointmentStatusRequest(AppointmentStatus.CONFIRMED));
        mvc.perform(put("/api/appointments/{id}/status", id).contentType(APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict());
        ServiceRecord record = new ServiceRecord();
        record.setVehicle(vehicle);
        record.setAppointment(appointments.findById(id).orElseThrow());
        record.setPerformedBy(staff);
        record.setServiceType(ServiceType.MAINTENANCE);
        record.setServiceDate(LocalDate.now());
        record.setMileage(45000);
        record.setDescription("Yağ değişimi");
        record.setCost(new BigDecimal("1250.00"));
        records.saveAndFlush(record);
        mvc.perform(put("/api/appointments/{id}/status", id).contentType(APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(put("/api/appointments/{id}/status", id).contentType(APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void malformedInputMissingUserAndInvalidIdsReturnStructuredErrors() throws Exception {
        Long id = create(vehicle).id();
        mvc.perform(put("/api/appointments/{id}/status", id).contentType(APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/appointments/{id}/staff", id).contentType(APPLICATION_JSON)
                        .content("{\"staffId\":999999}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/appointments").param("ownerId", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/appointments"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/appointments").param("ownerId", owner.getId().toString()).param("status", "DONE"))
                .andExpect(status().isBadRequest());
        assertThat(appointments.findById(id).orElseThrow().getAssignedStaff()).isNull();
    }

    @Test
    void concurrentOwnersCannotReserveSameSlot() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Integer> first = () -> attemptCreate(vehicle, start);
            Callable<Integer> second = () -> attemptCreate(otherVehicle, start);
            Future<Integer> one = executor.submit(first);
            Future<Integer> two = executor.submit(second);
            start.countDown();
            assertThat(List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(appointments.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Integer attemptCreate(Vehicle item, CountDownLatch start) throws Exception {
        start.await();
        try {
            create(item);
            return 201;
        } catch (ResponseStatusException failure) {
            return failure.getStatusCode().value();
        }
    }

    private AppointmentResponse create(Vehicle item) {
        return service.createAppointment(new CreateAppointmentRequest(
                item.getOwner().getId(), item.getId(), time, "  Yağ bakımı  "));
    }

    private String createJson(Long ownerId, Long vehicleId, LocalDateTime dateTime) {
        return "{\"ownerId\":" + ownerId + ",\"vehicleId\":" + vehicleId
                + ",\"appointmentDateTime\":\"" + dateTime
                + "\",\"requestDescription\":\"  Yağ bakımı  \"}";
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
