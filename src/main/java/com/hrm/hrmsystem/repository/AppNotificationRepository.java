package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    List<AppNotification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId);
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AppNotification a WHERE a.recipient.id = :recipientId")
    void deleteByRecipientId(@Param("recipientId") Long recipientId);
}
