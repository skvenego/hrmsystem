package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.PasswordResetOTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetOTPRepository extends JpaRepository<PasswordResetOTP, Long> {
    Optional<PasswordResetOTP> findByEmailAndUsedFalseAndExpiresAtAfter(String email, LocalDateTime now);
    Optional<PasswordResetOTP> findByEmailAndOtpAndUsedFalseAndExpiresAtAfter(String email, String otp, LocalDateTime now);

    @Modifying
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    @Query("DELETE FROM PasswordResetOTP p WHERE p.email = ?1")
    void deleteByEmail(String email);
}
