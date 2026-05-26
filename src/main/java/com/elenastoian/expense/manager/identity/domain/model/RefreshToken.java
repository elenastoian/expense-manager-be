package com.elenastoian.expense.manager.identity.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * A single-use opaque token used to authorise a password reset.
 *
 * Unlike JwtToken, this is not a JWT — it's a random UUID stored in full
 * because it's never transmitted in an Authorization header and has no
 * cryptographic self-validation. The server looks it up by value, checks
 * expiry and the used flag, then deletes it after use.
 */
@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID token;

    @Column
    private String tokenId; // jti claim of the associated JWT, used for blacklisting

    @Column(nullable = false)
    private boolean revoked;

    @Column(nullable = false)
    private boolean expired;

    @Column(nullable = false)
    private boolean used;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id", nullable = false)
    private User user;
}