package com.elenastoian.expense.manager.identity.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single-use opaque token sent via email to confirm a user's address.
 *
 * Same design rationale as PasswordResetToken — stored in full as a plain
 * UUID, looked up by value, and deleted after confirmation.
 */
@Entity
@Table(name = "email_confirmation_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailConfirmationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id", nullable = false)
    private User user;
}