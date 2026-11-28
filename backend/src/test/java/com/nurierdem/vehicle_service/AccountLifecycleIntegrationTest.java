package com.nurierdem.vehicle_service;

import com.nurierdem.vehicle_service.entity.User;
import com.nurierdem.vehicle_service.enums.UserRole;
import com.nurierdem.vehicle_service.repository.AppointmentRepository;
import com.nurierdem.vehicle_service.repository.ServiceRecordRepository;
import com.nurierdem.vehicle_service.repository.UserRepository;
import com.nurierdem.vehicle_service.repository.VehicleRepository;
import com.nurierdem.vehicle_service.service.JwtService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfSystemProperty(named = "test.db.url", matches = "jdbc:postgresql://127\\.0\\.0\\.1:.*")
@SpringBootTest(properties = {
        "spring.datasource.url=${test.db.url}", "spring.datasource.username=backend_test",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false",
        "APP_JWT_SECRET=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
class AccountLifecycleIntegrationTest {
    private static final String PASSWORD = "Deneme-Parola-78!";
    private final JsonMapper json = JsonMapper.builder().build();
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter springSecurity;
    @Autowired UserRepository users;
    @Autowired VehicleRepository vehicles;
    @Autowired AppointmentRepository appointments;
    @Autowired ServiceRecordRepository serviceRecords;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService tokens;
    MockMvc api;
    User admin;

    @BeforeEach
    void setup() {
        serviceRecords.deleteAll();
        appointments.deleteAll();
        vehicles.deleteAll();
        users.deleteAll();
        admin = new User();
        admin.setFullName("Deneme Yönetici");
        admin.setEmail("admin@example.test");
        admin.setRole(UserRole.ADMIN);
        admin.setPasswordHash(encoder.encode(PASSWORD));
        admin = users.saveAndFlush(admin);
        api = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurity).build();
    }

    @Test
    void registrationForcesCustomerRoleAndDuplicateEmailsKeepTheExistingAccount() throws Exception {
        api.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content("""
                {"fullName":"Ece Arslan","email":"Ece@Example.test","password":"Deneme-Parola-78!","role":"ADMIN"}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertThat(users.findByEmail("ece@example.test")).isPresent();

        api.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content("""
                {"fullName":"Yeni kişi","email":"ECE@EXAMPLE.test","password":"Baska-Parola-78!"}
                """)).andExpect(status().isConflict());
        assertThat(users.count()).isEqualTo(2);

        String customer = login("ECE@example.test", PASSWORD);
        api.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("ece@example.test"));
        api.perform(get("/api/vehicles").param("ownerId", admin.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(customer))).andExpect(status().isForbidden());
        api.perform(get("/api/vehicles/service").header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isForbidden());
        api.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isForbidden());
        api.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(customer))
                .contentType(APPLICATION_JSON).content(accountBody("STAFF"))).andExpect(status().isForbidden());
        api.perform(post("/api/users").contentType(APPLICATION_JSON).content(accountBody("USER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void temporaryPasswordCreatesRestrictedSessionThenReplacesItWithNormalStaffSession() throws Exception {
        String adminToken = bearer(login(admin.getEmail(), PASSWORD));
        api.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, adminToken)
                .contentType(APPLICATION_JSON).content(accountBody("ADMIN"))).andExpect(status().isBadRequest());

        JsonNode managed = body(api.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, adminToken)
                .contentType(APPLICATION_JSON).content(accountBody("STAFF"))).andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store")).andReturn());
        String temporary = managed.get("temporaryPassword").asString();
        Long staffId = managed.get("user").get("id").asLong();
        String pendingToken = bearer(login("staff@example.test", temporary));
        api.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, pendingToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(true));
        api.perform(get("/api/appointments/assigned").param("staffId", staffId.toString())
                .header(HttpHeaders.AUTHORIZATION, pendingToken)).andExpect(status().isForbidden());

        JsonNode renewed = body(api.perform(post("/api/users/{id}/temporary-password", staffId)
                .header(HttpHeaders.AUTHORIZATION, adminToken)).andExpect(status().isOk()).andReturn());
        api.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, pendingToken))
                .andExpect(status().isUnauthorized());
        String currentPendingToken = bearer(login("staff@example.test", renewed.get("temporaryPassword").asString()));
        JsonNode activated = body(api.perform(post("/api/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, currentPendingToken).contentType(APPLICATION_JSON)
                .content("{\"password\":\"Yeni-Personel-Sifre-79!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.mustChangePassword").value(false)).andReturn());

        api.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, currentPendingToken))
                .andExpect(status().isUnauthorized());
        api.perform(get("/api/appointments/assigned").param("staffId", staffId.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(activated.get("accessToken").asString())))
                .andExpect(status().isOk());
        api.perform(get("/api/appointments/assigned").param("staffId", admin.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(activated.get("accessToken").asString())))
                .andExpect(status().isForbidden());
        api.perform(post("/api/users/{id}/temporary-password", staffId).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCanCreateCustomerAndCurrentDatabaseRoleControlsAuthorization() throws Exception {
        String signedAsAdmin = bearer(tokens.createAccessToken(admin));
        api.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, signedAsAdmin)
                .contentType(APPLICATION_JSON).content(accountBody("USER")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.user.role").value("USER"));
        api.perform(get("/api/appointments/service").header(HttpHeaders.AUTHORIZATION, signedAsAdmin))
                .andExpect(status().isOk());

        admin.setRole(UserRole.USER);
        users.saveAndFlush(admin);
        api.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, signedAsAdmin))
                .andExpect(status().isForbidden());
        api.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, signedAsAdmin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void ownerReadsMaintenanceAndDeletingOwnerPreservesTheVehicleAndRecords() throws Exception {
        User owner = saveAccount("owner@example.test", UserRole.USER);
        User other = saveAccount("other@example.test", UserRole.USER);
        User staff = saveAccount("staff@example.test", UserRole.STAFF);
        String adminToken = bearer(tokens.createAccessToken(admin));
        String ownerToken = bearer(login(owner.getEmail(), PASSWORD));
        String otherToken = bearer(tokens.createAccessToken(other));
        long vehicleId = body(api.perform(post("/api/vehicles")
                .header(HttpHeaders.AUTHORIZATION, ownerToken).contentType(APPLICATION_JSON)
                .content("""
                        {"ownerId":%d,"plate":"58ABC123","brand":"Renault","model":"Clio",
                         "modelYear":2022,"fuelType":"GASOLINE","currentMileage":45000}
                        """.formatted(owner.getId())))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        LocalDateTime appointmentTime = LocalDate.now().plusDays(2).atTime(10, 0);
        long appointmentId = body(api.perform(post("/api/appointments")
                .header(HttpHeaders.AUTHORIZATION, ownerToken).contentType(APPLICATION_JSON)
                .content("""
                        {"vehicleId":%d,"ownerId":%d,"appointmentDateTime":"%s","requestDescription":"Periyodik bakım"}
                        """.formatted(vehicleId, owner.getId(), appointmentTime)))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        api.perform(post("/api/service-records/service")
                .header(HttpHeaders.AUTHORIZATION, adminToken).contentType(APPLICATION_JSON)
                .content("""
                        {"vehicleId":%d,"performedById":%d,"serviceType":"MAINTENANCE",
                         "serviceDate":"%s","mileage":46000,"description":"Yağ bakımı","cost":800}
                        """.formatted(vehicleId, staff.getId(), LocalDate.now())))
                .andExpect(status().isCreated());

        api.perform(get("/api/vehicles/{id}/service-records", vehicleId)
                .param("ownerId", owner.getId().toString()).header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].description").value("Yağ bakımı"))
                .andExpect(jsonPath("$[0].mileage").value(46000));
        api.perform(get("/api/vehicles/{id}/service-records", vehicleId)
                .param("ownerId", other.getId().toString()).header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isNotFound());
        api.perform(get("/api/vehicles/{id}/service-records", vehicleId)
                .param("ownerId", owner.getId().toString()).header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isForbidden());
        api.perform(delete("/api/users/{id}", owner.getId()).header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isForbidden());
        api.perform(delete("/api/users/{id}", admin.getId()).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isForbidden());
        api.perform(delete("/api/users/{id}", owner.getId()).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNoContent());
        api.perform(delete("/api/users/{id}", owner.getId()).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNoContent());
        api.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.email == 'owner@example.test')]").isEmpty());
        api.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isUnauthorized());
        api.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                .content(json.createObjectNode().put("email", owner.getEmail()).put("password", PASSWORD).toString()))
                .andExpect(status().isUnauthorized());
        api.perform(post("/api/users/{id}/temporary-password", owner.getId()).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound());
        api.perform(get("/api/vehicles/{id}/service-records/service", vehicleId)
                .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].description").value("Yağ bakımı"));
        api.perform(get("/api/appointments/service/{id}", appointmentId)
                .header(HttpHeaders.AUTHORIZATION, adminToken)).andExpect(status().isOk());
        assertThat(users.findById(owner.getId()).orElseThrow().isArchived()).isTrue();
    }

    private User saveAccount(String email, UserRole role) {
        User user = new User();
        user.setFullName(role.name());
        user.setEmail(email);
        user.setRole(role);
        user.setPasswordHash(encoder.encode(PASSWORD));
        return users.saveAndFlush(user);
    }

    private String accountBody(String role) {
        return """
                {"fullName":"Deneme Hesabı","email":"staff@example.test","role":"%s"}
                """.formatted(role);
    }

    private String login(String email, String password) throws Exception {
        return body(api.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                .content(json.createObjectNode().put("email", email).put("password", password).toString()))
                .andExpect(status().isOk()).andReturn()).get("accessToken").asString();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
