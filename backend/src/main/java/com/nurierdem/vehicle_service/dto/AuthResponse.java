package com.nurierdem.vehicle_service.dto;

import com.nurierdem.vehicle_service.entity.User;
import com.nurierdem.vehicle_service.enums.UserRole;

public record AuthResponse(String accessToken, UserInfo user) {
    public record UserInfo(Long id, String fullName, String email, UserRole role,
                           boolean mustChangePassword, boolean passwordConfigured) {
        public static UserInfo from(User user) {
            return new UserInfo(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                    user.isMustChangePassword(), user.hasPassword());
        }
    }
}
