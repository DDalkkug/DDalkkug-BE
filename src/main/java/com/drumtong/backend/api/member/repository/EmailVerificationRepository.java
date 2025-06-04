package com.drumtong.backend.api.member.repository;

import com.drumtong.backend.api.member.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmail(String email);
    Optional<EmailVerification> findByCode(String code);
}