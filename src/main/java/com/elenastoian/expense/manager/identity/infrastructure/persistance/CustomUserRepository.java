package com.elenastoian.expense.manager.identity.infrastructure.persistance;

import com.elenastoian.expense.manager.identity.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
