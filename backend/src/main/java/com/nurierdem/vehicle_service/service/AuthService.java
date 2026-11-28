package com.nurierdem.vehicle_service.service;

import com.nurierdem.vehicle_service.dto.LoginRequest;
import com.nurierdem.vehicle_service.entity.User;
import com.nurierdem.vehicle_service.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String fallbackHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fallbackHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public User authenticate(LoginRequest request) {
        String password = request.password();
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalidCredentials();
        }

        User user = userRepository.findByEmailIgnoreCase(request.email().strip())
                .filter(account -> !account.isArchived() && account.hasPassword())
                .orElse(null);
        boolean matches = passwordEncoder.matches(password, user == null ? fallbackHash : user.getPasswordHash());
        if (user == null || !matches) {
            throw invalidCredentials();
        }

        return user;
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-posta adresi veya şifre hatalı.");
    }
}
