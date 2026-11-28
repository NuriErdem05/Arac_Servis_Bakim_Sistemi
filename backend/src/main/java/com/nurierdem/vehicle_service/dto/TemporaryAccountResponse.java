package com.nurierdem.vehicle_service.dto;

public record TemporaryAccountResponse(AuthResponse.UserInfo user, String temporaryPassword) {
}
