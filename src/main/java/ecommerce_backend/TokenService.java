package ecommerce_backend;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final long ttlSeconds;

    public TokenService(JwtEncoder jwtEncoder, @Value("${app.jwt.ttl-minutes:60}") long ttlMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.ttlSeconds = Duration.ofMinutes(ttlMinutes).toSeconds();
    }

    public LoginResponse issue(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ecommerce-backend")
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .claim("roles", List.of(user.getRole()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(token, user.getRole(), ttlSeconds);
    }
}
