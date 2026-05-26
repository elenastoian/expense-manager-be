package com.elenastoian.expense.manager.identity.domain.repository;

import com.elenastoian.expense.manager.identity.domain.model.EmailConfirmationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailConfirmationTokenRepository extends JpaRepository<EmailConfirmationToken, Long> {

    Optional<EmailConfirmationToken> findByToken(String token);

    void deleteAllByUserId(Long userId);
}