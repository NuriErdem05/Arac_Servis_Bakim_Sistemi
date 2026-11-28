package com.nurierdem.vehicle_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeInitialPasswordRequest(
        @NotBlank(message = "Yeni şifre zorunludur.")
        @Size(min = 8, max = 72, message = "Şifre 8 ile 72 karakter arasında olmalı.") String password
) {
}
