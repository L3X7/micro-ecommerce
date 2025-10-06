package com.microecommerce.user_service.service;

import com.microecommerce.user_service.model.RefreshToken;
import com.microecommerce.user_service.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public void CreateRefreshTokenData(String username, String jti, Instant now, Instant exp, String tokenRefreshJwt) {
        RefreshToken record = new RefreshToken();
        record.setJti(jti);
        record.setSubject(username);
        record.setIssuedAt(now);
        record.setExpiresAt(exp);
        record.setTokenHash(tokenRefreshJwt);
        refreshTokenRepository.save(record);
    }
}
