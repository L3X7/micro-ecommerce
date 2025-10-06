package com.microecommerce.user_service.repository;

import com.microecommerce.user_service.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByJti(String jti);

    List<RefreshToken> findAllBySubjectAndRevokedFalse(String subject);
}
