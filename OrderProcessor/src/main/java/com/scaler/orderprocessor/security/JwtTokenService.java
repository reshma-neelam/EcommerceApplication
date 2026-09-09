package com.scaler.orderprocessor.security;

import com.scaler.orderprocessor.config.JwtProperties;
import com.scaler.orderprocessor.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService implements TokenService {

    private final SecretKey key;
    private final String issuer;

    public JwtTokenService(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        this.issuer = props.getIssuer();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ParsedToken parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            List<String> roleList = claims.get("roles", List.class);
            Set<String> roles = roleList == null ? Set.of() : new HashSet<>(roleList);
            return new ParsedToken(userId, roles);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("UNAUTHORIZED", "Invalid or expired token");
        }
    }
}
