package com.finance_tracker.utils.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long!";

    private JwtService jwtService() {
        JwtService svc = new JwtService();
        ReflectionTestUtils.setField(svc, "secretKey", SECRET);
        return svc;
    }

    // ── generateToken ─────────────────────────────────────────────────────────

    @Test
    void generateToken_returnsNonBlankToken() {
        String token = jwtService().generateToken(1L);
        assertThat(token).isNotBlank();
    }

    @Test
    void generateToken_differentUsers_differentTokens() {
        JwtService svc = jwtService();
        String t1 = svc.generateToken(1L);
        String t2 = svc.generateToken(2L);

        assertThat(t1).isNotEqualTo(t2);
    }

    // ── extractUsername ───────────────────────────────────────────────────────

    @Test
    void extractUsername_validToken_returnsSubject() {
        JwtService svc = jwtService();
        String token = svc.generateToken(42L);

        assertThat(svc.extractUsername(token)).isEqualTo("42");
    }

    @Test
    void extractUsername_malformedToken_returnsNull() {
        assertThat(jwtService().extractUsername("not.a.jwt")).isNull();
    }

    @Test
    void extractUsername_emptyString_returnsNull() {
        assertThat(jwtService().extractUsername("")).isNull();
    }

    @Test
    void extractUsername_wrongSignature_returnsNull() {
        JwtService svc1 = new JwtService();
        ReflectionTestUtils.setField(svc1, "secretKey", "first-secret-key-that-is-at-least-32-bytes");

        JwtService svc2 = new JwtService();
        ReflectionTestUtils.setField(svc2, "secretKey", "other-secret-key-that-is-at-least-32-bytes");

        String token = svc1.generateToken(1L);
        assertThat(svc2.extractUsername(token)).isNull();
    }

    // ── isTokenValid ──────────────────────────────────────────────────────────

    @Test
    void isTokenValid_validToken_returnsTrue() {
        JwtService svc = jwtService();
        String token = svc.generateToken(1L);

        assertThat(svc.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_malformedToken_returnsFalse() {
        assertThat(jwtService().isTokenValid("garbage.token.here")).isFalse();
    }

    @Test
    void isTokenValid_emptyToken_returnsFalse() {
        assertThat(jwtService().isTokenValid("")).isFalse();
    }

    @Test
    void isTokenValid_wrongSignature_returnsFalse() {
        JwtService svc1 = new JwtService();
        ReflectionTestUtils.setField(svc1, "secretKey", "first-secret-key-that-is-at-least-32-bytes");

        JwtService svc2 = new JwtService();
        ReflectionTestUtils.setField(svc2, "secretKey", "other-secret-key-that-is-at-least-32-bytes");

        String token = svc1.generateToken(1L);
        assertThat(svc2.isTokenValid(token)).isFalse();
    }

    // ── extractAllClaims ──────────────────────────────────────────────────────

    @Test
    void extractAllClaims_validToken_returnsClaims() {
        JwtService svc = jwtService();
        String token = svc.generateToken(7L);

        var claims = svc.extractAllClaims(token);

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.getExpiration()).isInTheFuture();
    }

    @Test
    void extractAllClaims_malformedToken_throws() {
        assertThatThrownBy(() -> jwtService().extractAllClaims("bad.token"))
                .isInstanceOf(Exception.class);
    }
}
