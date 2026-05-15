package com.elenastoian.expense.manager.identity.domain.model;

import com.elenastoian.expense.manager.identity.domain.model.enums.TokenType;
import jakarta.persistence.*;
import lombok.*;

/**
 * This entity represents a JWT token issued to a user.
 * Instead of storing the full JWT string, we only store the unique identifier (jti claim) of the token.
 * This allows us to efficiently check if a token has been revoked or expired without needing to parse the entire JWT.
 * The Token entity is linked to the AppUser entity, allowing us to track which tokens belong to which users.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stores the jti (JWT ID) UUID claim — NOT the full JWT string.
     * This keeps the DB lean and avoids exposing valid credentials if breached.
     */
    @Column(nullable = false, unique = true)
    private String tokenId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenType tokenType;

    @Column(nullable = false)
    private boolean revoked;

    @Column(nullable = false)
    private boolean expired;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id", nullable = false)
    private User user;
}
