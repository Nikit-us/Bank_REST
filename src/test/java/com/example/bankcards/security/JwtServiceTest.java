package com.example.bankcards.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.example.bankcards.config.AppProperties;
import com.example.bankcards.entity.enums.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdGhhdC1pcy1sb25nLWVub3VnaC0yNTY=";

    private JwtService newService(long expirationMs) {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret(SECRET);
        props.getJwt().setExpirationMs(expirationMs);
        return new JwtService(props);
    }

    @Test
    void generatesAndParsesToken() {
        JwtService service = newService(3_600_000L);

        String token = service.generateToken("alice", Role.USER, 42L);
        Claims claims = service.parseClaims(token);

        assertAll(
                () -> assertThat(claims.getSubject()).isEqualTo("alice"),
                () -> assertThat(claims.get("role", String.class)).isEqualTo("USER"),
                () -> assertThat(claims.get("uid", Long.class)).isEqualTo(42L),
                () -> assertThat(service.extractUsername(token)).isEqualTo("alice")
        );
    }

    @Test
    void expiredTokenIsNotAccepted() {
        JwtService service = newService(-1_000L);

        String token = service.generateToken("bob", Role.ADMIN, 1L);

        assertThat(service.extractUsername(token)).isNull();
    }

    @Test
    void tamperedTokenIsNotAccepted() {
        JwtService service = newService(3_600_000L);

        String token = service.generateToken("carol", Role.USER, 7L);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(service.extractUsername(tampered)).isNull();
    }
}
