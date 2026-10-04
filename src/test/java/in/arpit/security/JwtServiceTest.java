package in.arpit.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import in.arpit.entity.AuthProvider;
import in.arpit.entity.Role;
import in.arpit.entity.User;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    private User user() {
        return User.builder().id(7L).name("Arpit").email("arpit@example.com")
                .role(Role.OWNER).provider(AuthProvider.LOCAL).enabled(true).build();
    }

    @Test
    void tokenRoundTripContainsExpectedClaims() {
        JwtService jwt = new JwtService(SECRET, 60_000);
        Jwt parsed = jwt.parse(jwt.generateToken(user()));
        assertEquals("arpit@example.com", parsed.getSubject());
        assertEquals("OWNER", parsed.getClaimAsString("role"));
        assertEquals(7L, ((Number) parsed.getClaim("userId")).longValue());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, 60_000);
        String token = jwt.generateToken(user());
        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThrows(JwtException.class, () -> jwt.parse(tampered));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtService("another-secret-another-secret-123456", 60_000).generateToken(user());
        assertThrows(JwtException.class, () -> new JwtService(SECRET, 60_000).parse(token));
    }

    @Test
    void expiredTokenIsRejected() {
        // negative lifetime => already expired (the decoder allows 60s clock skew, so go well past it)
        JwtService jwt = new JwtService(SECRET, -120_000);
        String token = jwt.generateToken(user());
        assertThrows(JwtException.class, () -> jwt.parse(token));
    }

    @Test
    void shortSecretIsRefused() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 60_000));
    }
}
