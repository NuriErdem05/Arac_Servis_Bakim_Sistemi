package com.nurierdem.vehicle_service.config;

import com.nurierdem.vehicle_service.security.AccountJwtAuthenticationConverter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, AccountJwtAuthenticationConverter accounts) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(routes -> routes
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/change-password").hasRole("PASSWORD_CHANGE")
                        .requestMatchers("/api/users", "/api/users/**",
                                "/api/vehicles/service/**",
                                "/api/appointments/service/**",
                                "/api/appointments/{id}/staff", "/api/appointments/{id}/status",
                                "/api/service-records/service",
                                "/api/vehicles/{id}/service-records/service").hasRole("ADMIN")
                        .requestMatchers("/api/appointments/assigned",
                                "/api/service-records",
                                "/api/vehicles/{id}/service-records/assigned").hasRole("STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/vehicles", "/api/appointments")
                                .hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/vehicles/**", "/api/appointments/**").hasRole("USER")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(accounts)))
                .build();
    }
}
