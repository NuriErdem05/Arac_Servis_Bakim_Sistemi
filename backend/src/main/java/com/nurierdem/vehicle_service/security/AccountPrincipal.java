package com.nurierdem.vehicle_service.security;

import com.nurierdem.vehicle_service.dto.AuthResponse.UserInfo;

public record AccountPrincipal(UserInfo user, long credentialVersion) {
}
