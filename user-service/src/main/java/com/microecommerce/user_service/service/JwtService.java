package com.microecommerce.user_service.service;

import com.microecommerce.user_service.model.RefreshToken;
import com.microecommerce.user_service.repository.RefreshTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${JWT_SECRET_KEY}")
    private String secretKeyRaw;

    @Value("${JWT_SECRET_ENCODING}") // "base64" or "plain"
    private String secretEncoding;

    @Value("${JWT_ISSUER}")
    private String issuer;

    @Value("${JWT_ACCESS_TTL_MIN}")
    private long jwtAccessTtlMinutes;

    @Value("${JWT_REFRESH_ACCESS_TTL_DAYS}")
    private long jwtRefreshAccessTtlDays;

    private SecretKey signingKey;

    private JwtParser parser;

    private static final HexFormat HEX = HexFormat.of().withLowerCase();
    private final RefreshTokenRepository refreshTokenRepository;

    @PostConstruct
    void init() {
        String s = sanitize(secretKeyRaw);
        byte[] keyBytes = "plain".equalsIgnoreCase(secretEncoding)
                ? s.getBytes(StandardCharsets.UTF_8)
                : Decoders.BASE64.decode(s);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes); // HS256 (>=256-bit)

        this.parser = Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(signingKey)
                .clockSkewSeconds(60)  // optional tolerance
                .build();
    }

    public String generateToken(String username, Map<String, Object> extraClaims) {
        Instant now = Instant.now();

        Map<String, Object> claims = new HashMap<>();
        if (extraClaims != null) claims.putAll(extraClaims);
        claims.putIfAbsent("token_type", "access");

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(jwtAccessTtlMinutes))))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(String username, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        if (extraClaims != null) claims.putAll(extraClaims);
        claims.putIfAbsent("token_type", "refresh");
        String jti = UUID.randomUUID().toString();

        String refreshToken = Jwts.builder()
                .claims(claims)
                .subject(username)
                .id(jti)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofDays(jwtRefreshAccessTtlDays))))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        RefreshToken rt = new RefreshToken();
        rt.setJti(jti);
        rt.setSubject(username);
        rt.setIssuedAt(now);
        rt.setExpiresAt(now.plus(Duration.ofDays(jwtRefreshAccessTtlDays)));
        rt.setTokenHash(sha256Helper(refreshToken));
        refreshTokenRepository.save(rt);

        return refreshToken;
    }

    public String generateNewRefreshToken(String oldRefreshToken) {
        Claims c = extractAllClaims(oldRefreshToken);
        String subject = c.getSubject();
        String jti = c.getId();
        String tokenType = c.get("token_type", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new IllegalStateException("Token is not a refresh token");
        }
        RefreshToken refreshToken = refreshTokenRepository.findByJti(jti).orElseThrow(() -> new IllegalStateException("Unknown refresh token."));
        if (refreshToken.isRevoked()) {
            throw new IllegalStateException("Refresh token reuse detected.");
        }

        if (!sha256Helper(oldRefreshToken).equals(refreshToken.getTokenHash())) {
            throw new IllegalStateException("Refresh token mismatch.");
        }

        if (isTokenExpired(oldRefreshToken)) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new IllegalStateException("Refresh token expired.");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return generateRefreshToken(subject, null);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }


    public Claims extractAllClaims(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims c = extractAllClaims(token);
        String tokenType = c.get("token_type", String.class);
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && "access".equals(tokenType) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        try {
            Date exp = extractClaim(token, Claims::getExpiration);
            return exp.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        s = s.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    public static String sha256Helper(String raw) {
        if (raw == null) throw new IllegalArgumentException("raw token is null");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(digest); // 64-char lowercase hex
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

}
