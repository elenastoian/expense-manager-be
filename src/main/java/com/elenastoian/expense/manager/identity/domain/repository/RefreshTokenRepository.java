package com.elenastoian.expense.manager.identity.domain.repository;

import com.elenastoian.expense.manager.identity.domain.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

     Optional<RefreshToken> findByTokenId(String tokenId);

        @Query("SELECT t FROM RefreshToken t WHERE t.user.id = :userId AND t.expired = false AND t.revoked = false")
        List<RefreshToken> findAllValidTokensByUser(Long userId);
}
