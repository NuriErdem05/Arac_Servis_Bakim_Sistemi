package com.nurierdem.vehicle_service;

import com.nurierdem.vehicle_service.entity.*;
import com.nurierdem.vehicle_service.enums.*;
import com.nurierdem.vehicle_service.repository.*;
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

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Data scope and business rules with a separate security-chain test.
@EnabledIfSystemProperty(named = "test.db.url", matches = "jdbc:postgresql://127\\.0\\.0\\.1:.*")
@Import(BusinessTestAccountScope.class)
@SpringBootTest(properties = {
        "APP_JWT_SECRET=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false",
        "spring.datasource.username=backend_test", "spring.datasource.password=", "spring.sql.init.mode=never"
})
class AdminDataScopeIntegrationTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getProperty("test.db.url"));
    }

    @Autowired VehicleRepository vehicles;
    @Autowired UserRepository users;
    @Autowired AppointmentRepository appointments;
    @Autowired ServiceRecordRepository records;
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;
    User owner, otherOwner, staff;
    Vehicle first, second;

    @BeforeEach
    void setup() {
        records.deleteAll(); appointments.deleteAll(); vehicles.deleteAll(); users.deleteAll();
        owner = saveUser("Sahip", "owner@example.test");
        otherOwner = saveUser("Diğer sahip", "other@example.test");
        staff = saveUser("Personel", "staff@example.test");
        first = saveVehicle(owner, "58ABC123");
        second = saveVehicle(otherOwner, "58DBC123");
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void adminReadsBothOwnersWhileOwnerRouteKeepsScopeAndArchiveFilter() throws Exception {
        Vehicle archived = saveVehicle(owner, "58OLD123");
        archived.setArchived(true); vehicles.saveAndFlush(archived);
        mvc.perform(get("/api/vehicles/service")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ownerId").value(otherOwner.getId()));
        mvc.perform(get("/api/vehicles/service/{id}", second.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.plate").value("58DBC123"));
        mvc.perform(get("/api/vehicles").param("ownerId", owner.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/vehicles/{id}", second.getId()).param("ownerId", owner.getId().toString()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/vehicles/service/{id}", archived.getId())).andExpect(status().isNotFound());
    }

    @Test
    void adminEditsAnyActiveVehicleWithSameMileagePlateAndYearRules() throws Exception {
        mvc.perform(put("/api/vehicles/service/{id}", second.getId()).contentType(APPLICATION_JSON)
                        .content(vehicleJson("58DBC123", 50000, 2022)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value(otherOwner.getId()))
                .andExpect(jsonPath("$.currentMileage").value(50000));
        mvc.perform(put("/api/vehicles/service/{id}", second.getId()).contentType(APPLICATION_JSON)
                        .content(vehicleJson("58DBC123", 49999, 2022))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/vehicles/service/{id}", second.getId()).contentType(APPLICATION_JSON)
                        .content(vehicleJson("58ABC123", 50000, 2022))).andExpect(status().isConflict());
        mvc.perform(put("/api/vehicles/service/{id}", second.getId()).contentType(APPLICATION_JSON)
                        .content(vehicleJson("58DBC123", 50000, Year.now().getValue() + 2)))
                .andExpect(status().isBadRequest());
        assertThat(vehicles.findById(second.getId()).orElseThrow().getCurrentMileage()).isEqualTo(50000);
    }

    @Test
    void adminCreatesVehicleForSelectedOwnerAndCannotUseMissingOwner() throws Exception {
        String body = vehicleJson("58NEW123", 0, 2022);
        mvc.perform(post("/api/vehicles").contentType(APPLICATION_JSON)
                        .content(body.replaceFirst("\\{", "{\"ownerId\":" + otherOwner.getId() + ",")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ownerId").value(otherOwner.getId()));
        mvc.perform(post("/api/vehicles").contentType(APPLICATION_JSON)
                        .content(body.replaceFirst("\\{", "{\"ownerId\":999999,")))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCreatesIndependentRecordForOtherVehicleWithoutChangingStaffScope() throws Exception {
        String body = recordJson(second.getId(), null, staff.getId(), 50000);
        mvc.perform(post("/api/service-records").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/service-records/service").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.performedById").value(staff.getId()))
                .andExpect(jsonPath("$.appointmentId").isEmpty());
        mvc.perform(get("/api/service-records/service")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/vehicles/{id}/service-records/service", second.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/vehicles/{id}/service-records", second.getId())
                        .param("ownerId", owner.getId().toString())).andExpect(status().isNotFound());
        assertThat(vehicles.findById(second.getId()).orElseThrow().getCurrentMileage()).isEqualTo(50000);
    }

    @Test
    void adminAppointmentFlowSharesAssignmentCompletionAndDuplicateRules() throws Exception {
        LocalDateTime time = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        String body = "{\"ownerId\":" + otherOwner.getId() + ",\"vehicleId\":" + second.getId()
                + ",\"appointmentDateTime\":\"" + time + "\",\"requestDescription\":\"Bakım\"}";
        mvc.perform(post("/api/appointments").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        Long id = appointments.findAll().getFirst().getId();
        mvc.perform(get("/api/appointments/service/{id}", id)).andExpect(status().isOk());
        mvc.perform(put("/api/appointments/{id}/staff", id).contentType(APPLICATION_JSON)
                        .content("{\"staffId\":" + staff.getId() + "}")).andExpect(status().isOk());
        mvc.perform(put("/api/appointments/{id}/status", id).contentType(APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/appointments/service").param("status", "CONFIRMED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(post("/api/service-records/service").contentType(APPLICATION_JSON)
                        .content(recordJson(second.getId(), id, owner.getId(), 50000)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/service-records/service").contentType(APPLICATION_JSON)
                        .content(recordJson(second.getId(), id, staff.getId(), 50000)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/service-records/service").contentType(APPLICATION_JSON)
                        .content(recordJson(second.getId(), id, staff.getId(), 50000)))
                .andExpect(status().isConflict());
        assertThat(appointments.findById(id).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void adminValidationAndMissingEntitiesProduceControlledErrors() throws Exception {
        mvc.perform(get("/api/vehicles/service/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/vehicles/service/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/appointments/service/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/appointments/service/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/appointments/service").param("status", "UNKNOWN")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/vehicles/999999/service-records/service")).andExpect(status().isNotFound());
        mvc.perform(post("/api/service-records/service").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.vehicleId").exists());
    }

    @Test
    void adminWriteRollsBackRecordAndMileageOnDatabaseFailure() throws Exception {
        jdbc.execute("alter table vehicles add constraint test_reject_admin_mileage check (current_mileage <> 50000)");
        try {
            mvc.perform(post("/api/service-records/service").contentType(APPLICATION_JSON)
                            .content(recordJson(second.getId(), null, staff.getId(), 50000)))
                    .andExpect(status().isConflict());
        } finally { jdbc.execute("alter table vehicles drop constraint test_reject_admin_mileage"); }
        assertThat(records.count()).isZero();
        assertThat(vehicles.findById(second.getId()).orElseThrow().getCurrentMileage()).isEqualTo(45000);
    }

    private String vehicleJson(String plate, int mileage, int year) {
        return "{\"plate\":\"" + plate + "\",\"brand\":\"Renault\",\"model\":\"Clio\",\"modelYear\":" + year
                + ",\"fuelType\":\"GASOLINE\",\"engineInfo\":\"1.0 TCe\",\"currentMileage\":" + mileage + "}";
    }

    private String recordJson(Long vehicle, Long appointment, Long performer, int mileage) {
        return "{\"vehicleId\":" + vehicle + ",\"appointmentId\":" + appointment + ",\"performedById\":" + performer
                + ",\"serviceType\":\"MAINTENANCE\",\"serviceDate\":\"" + LocalDate.now()
                + "\",\"mileage\":" + mileage + ",\"replacedParts\":\"Filtre\",\"description\":\"Bakım\",\"cost\":1000}";
    }

    private User saveUser(String name, String email) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setRole(email.contains("staff") ? UserRole.STAFF : UserRole.USER);
        return users.saveAndFlush(user);
    }

    private Vehicle saveVehicle(User user, String plate) {
        Vehicle vehicle = new Vehicle(); vehicle.setOwner(user); vehicle.setPlate(plate); vehicle.setBrand("Renault");
        vehicle.setModel("Clio"); vehicle.setModelYear(2022); vehicle.setFuelType(FuelType.GASOLINE);
        vehicle.setCurrentMileage(45000); return vehicles.saveAndFlush(vehicle);
    }
}
