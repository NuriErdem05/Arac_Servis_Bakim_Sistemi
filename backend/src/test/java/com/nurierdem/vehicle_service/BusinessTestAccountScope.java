package com.nurierdem.vehicle_service;

import com.nurierdem.vehicle_service.security.AccountAccess;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
class BusinessTestAccountScope {
    // These existing MVC tests exercise business rules without installing the security filter.
    // AccountLifecycleIntegrationTest exercises the live security chain separately.
    @Bean
    @Primary
    AccountAccess businessTestAccountAccess() {
        return new AccountAccess() {
            @Override public void requireSelf(Long id) { }
            @Override public void requireSelfOrAdmin(Long id) { }
        };
    }
}
