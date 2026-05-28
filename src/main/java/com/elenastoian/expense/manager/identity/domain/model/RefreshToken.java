package com.elenastoian.expense.manager.identity.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks a single active refresh-token session for a user.
 *
 * Only the JWT's jti claim (tokenId) is persisted — the raw token string is
 * never stored.  The server validates the JWT cryptographically first, then
 * cross-checks this record to enforce revocation and DB-side expiry.
 */
@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The jti claim of the associated JWT — used for lookup and revocation. */
    @Column(nullable = false, unique = true)
    private String tokenId;

    @Column(nullable = false)
    private boolean revoked;

    @Column(nullable = false)
    private boolean expired;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id", nullable = false)
    private User user;
}