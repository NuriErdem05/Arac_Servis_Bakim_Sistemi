package com.nurierdem.vehicle_service.security;

import com.nurierdem.vehicle_service.dto.AuthResponse.UserInfo;
import com.nurierdem.vehicle_service.entity.User;
import com.nurierdem.vehicle_service.repository.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccountJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserRepository users;

    public AccountJwtAuthenticationConverter(UserRepository users) {
        this.users = users;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        long userId;
        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException failure) {
            throw new InvalidBearerTokenException("Geçersiz kullanıcı kimliği.");
        }
        User user = users.findById(userId)
                .orElseThrow(() -> new InvalidBearerTokenException("Hesap bulunamadı."));

        Object claim = jwt.getClaim("credential_version");
        long version = claim instanceof Number number ? number.longValue() : 0;
        if (user.isArchived() || !user.hasPassword() || version != user.getCredentialVersion()) {
            throw new InvalidBearerTokenException("Oturum geçersiz. Yeniden giriş yapın.");
        }

        String authority = user.isMustChangePassword() ? "ROLE_PASSWORD_CHANGE" : "ROLE_" + user.getRole().name();
        var principal = new AccountPrincipal(UserInfo.from(user), version);
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority(authority)));
    }
}
