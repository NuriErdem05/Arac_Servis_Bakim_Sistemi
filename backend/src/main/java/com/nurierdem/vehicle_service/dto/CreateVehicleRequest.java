package com.nurierdem.vehicle_service.dto;

import com.nurierdem.vehicle_service.enums.FuelType;
import jakarta.validation.constraints.*;

public record CreateVehicleRequest(
        @NotNull(message = "Araç sahibi zorunludur.")
        @Positive(message = "Araç sahibi kimliği pozitif olmalıdır.")
        Long ownerId,

        @NotBlank(message = "Plaka zorunludur.")
        @Size(max = 20, message = "PLaka en fazla 20 karakter olabilir.")
        @Pattern(regexp = "[A-Za-z0-9 ]+", message = "Plaka yalnızca harf, rakam ve boşluk içerebilir.")
        String plate,

        @NotBlank(message = "Marka zorunludur.")
        @Size(max = 100, message = "Marka en fazla 100 karakter olabilir.")
        String brand,

        @NotBlank(message = "Model zorunludur.")
        @Size(max = 100, message = "Model en fazla 100 karakter olabilir.")
        String model,

        @NotNull(message = "Model yılı zorunludur.")
        @Min(value = 1886, message = "Geçerli bir model yılı girin.")
        Integer modelYear,

        @NotNull(message = "Yakıt türü zorunludur.")
        FuelType fuelType,

        @Size(max = 150, message = "Motor bilgisi en fazla 150 karakter olabilir.")
        String engineInfo,

        @NotNull(message = "Kilometre zorunludur.")
        @PositiveOrZero(message = "Kilometre negatif olamaz.")
        Integer currentMileage
) {
}
