package com.nurierdem.vehicle_service.dto;

import com.nurierdem.vehicle_service.enums.ServiceType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateServiceRecordRequest(
        @NotNull(message = "Araç seçimi zorunludur.")
        @Positive(message = "Araç kimliği pozitif olmalıdır.")
        Long vehicleId,

        @Positive(message = "Randevu kimliği pozitif olmalıdır.")
        Long appointmentId,

        @NotNull(message = "İşlemi yapan personel zorunludur.")
        @Positive(message = "Personel kimliği pozitif olmalıdır.")
        Long performedById,

        @NotNull(message = "İşlem türü zorunludur.")
        ServiceType serviceType,

        @NotNull(message = "İşlem tarihi zorunludur.")
        @PastOrPresent(message = "İşlem tarihi gelecekte olamaz.")
        LocalDate serviceDate,

        @NotNull(message = "Kilometre zorunludur.")
        @PositiveOrZero(message = "Kilometre negatif olamaz.")
        Integer mileage,

        @Size(max = 2000, message = "Değişen parçalar en fazla 2000 karakter olabilir.")
        String replacedParts,

        @NotBlank(message = "İşlem açıklaması zorunludur.")
        @Size(max = 2000, message = "İşlem açıklaması en fazla 2000 karakter olabilir.")
        String description,

        @NotNull(message = "Maliyet zorunludur.")
        @DecimalMin(value = "0.00", message = "Maliyet negatif olamaz.")
        @Digits(
                integer = 10,
                fraction = 2,
                message = "Maliyet en fazla 10 tam ve 2 ondalık basamak içerebilir."
        )
        BigDecimal cost
) {
}