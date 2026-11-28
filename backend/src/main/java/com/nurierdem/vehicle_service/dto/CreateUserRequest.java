package com.nurierdem.vehicle_service.dto;

import com.nurierdem.vehicle_service.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Ad soyad zorunludur.")
        @Size(max = 150, message = "Ad soyad en fazla 150 karakter olabilir.") String fullName,
        @NotBlank(message = "E-posta zorunludur.")
        @Email(message = "Geçerli bir e-posta yazın.")
        @Size(max = 254, message = "E-posta en fazla 254 karakter olabilir.") String email,
        @NotNull(message = "Hesap türü seçin.") UserRole role
) {
}
