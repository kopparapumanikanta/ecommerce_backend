package ecommerce_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtRoundTripTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    private final JwtConfig config = new JwtConfig();

    @Test
    void issuedToken_decodesWithSubjectAndRoles() {
        TokenService tokenService = new TokenService(config.jwtEncoder(SECRET), 5);
        LoginResponse response = tokenService.issue(new AppUser("store1", "hash", "STORE_USER"));

        Jwt jwt = config.jwtDecoder(SECRET).decode(response.token());

        assertEquals("store1", jwt.getSubject());
        assertEquals(List.of("STORE_USER"), jwt.getClaimAsStringList("roles"));
        assertEquals(300, response.expiresInSeconds());
    }

    @Test
    void tokenSignedWithDifferentSecret_isRejected() {
        TokenService tokenService = new TokenService(config.jwtEncoder("another-secret-another-secret-123456"), 5);
        String token = tokenService.issue(new AppUser("admin", "hash", "ADMIN")).token();
        JwtDecoder decoder = config.jwtDecoder(SECRET);

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void shortSecret_isRejected() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> config.jwtEncoder("short"));
        assertTrue(ex.getMessage().contains("32"));
    }
}
