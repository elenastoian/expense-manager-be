package com.elenastoian.expense.manager.identity.domain.repository;

import com.elenastoian.expense.manager.identity.domain.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteAllByUserId(Long userId);
}