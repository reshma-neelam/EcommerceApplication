package com.scaler.usermanagement.security;

import com.scaler.usermanagement.config.JwtProperties;
import com.scaler.usermanagement.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService implements TokenService {

    private final SecretKey key;
    private final long ttlSeconds;
    private final String issuer;

    public JwtTokenService(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = props.getAccessTokenTtlSeconds();
        this.issuer = props.getIssuer();
    }

    @Override
    public String createAccessToken(UUID userId, Set<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
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
