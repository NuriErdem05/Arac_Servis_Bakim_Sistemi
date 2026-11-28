package com.nurierdem.vehicle_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Ad soyad zorunludur.")
        @Size(max = 150, message = "Ad soyad en fazla 150 karakter olabilir.") String fullName,
        @NotBlank(message = "E-posta zorunludur.")
        @Email(message = "Geçerli bir e-posta yazın.")
        @Size(max = 254, message = "E-posta en fazla 254 karakter olabilir.") String email,
        @NotBlank(message = "Şifre zorunludur.")
        @Size(min = 8, max = 72, message = "Şifre 8 ile 72 karakter arasında olmalı.") String password
) {
}
