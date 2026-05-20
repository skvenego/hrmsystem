package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEmployeeId(Long employeeId);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<AuditLog> findByChangedBy(String changedBy);

    List<AuditLog> findByChangedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<AuditLog> findByEmployeeIdOrderByChangedAtDesc(Long employeeId);

    void deleteByEmployeeId(Long employeeId);
}
