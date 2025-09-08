package com.microecommerce.product_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${JWT_SECRET_KEY}")
    private String secretKeyRaw;

    @Value("${JWT_SECRET_ENCODING}") // "base64" or "plain"
    private String secretEncoding;

    @Value("${JWT_ISSUER}")
    private String issuer;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        String s = sanitize(secretKeyRaw);
        byte[] keyBytes = "plain".equalsIgnoreCase(secretEncoding)
                ? s.getBytes(StandardCharsets.UTF_8)
                : Decoders.BASE64.decode(s);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes); // HS256 (>=256-bit)
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public List<GrantedAuthority> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);

        List<String> roles = claims.get("roles", List.class);

        // If roles are not present, return an empty list
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        // Map the list of role strings to a list of SimpleGrantedAuthority objects
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    private Key getSigningKey() {
        return signingKey;
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .requireIssuer(issuer)
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String userId = extractUsername(token);
        return (userId.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        s = s.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }
}
