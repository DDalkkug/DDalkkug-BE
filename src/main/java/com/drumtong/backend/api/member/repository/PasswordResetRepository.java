package com.drumtong.backend.api.member.repository;

import com.drumtong.backend.api.member.entity.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findByCode(String code);
}
