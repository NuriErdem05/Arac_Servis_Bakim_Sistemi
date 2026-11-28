package com.nurierdem.vehicle_service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

@EnabledIfSystemProperty(named = "test.db.url", matches = "jdbc:postgresql://127\\.0\\.0\\.1:.*")
@SpringBootTest(properties = {
        "spring.datasource.url=${test.db.url}", "spring.datasource.username=backend_test",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "APP_JWT_SECRET=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
class VehicleServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
