package com.nurierdem.vehicle_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

@Configuration
public class JwtConfig {
    public static final String ISSUER = "vehicle-service";

    @Bean
    SecretKey jwtSigningKey(@Value("${APP_JWT_SECRET}") String base64Secret) {
        byte[] secret = Base64.getDecoder().decode(base64Secret);
        if (secret.length < 32) {
            throw new IllegalArgumentException("APP_JWT_SECRET en az 32 baytlık bir anahtar içermeli.");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSigningKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER),
                new JwtClaimValidator<Instant>(JwtClaimNames.EXP, Objects::nonNull)));
        return decoder;
    }
}
