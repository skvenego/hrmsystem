package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.model.AuditLog;
import com.hrm.hrmsystem.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Log an audit entry for any entity change
     */
    public void logChange(String entityType, Long entityId, String action, 
                         String oldValue, String newValue, Long employeeId, String remarks) {
        String changedBy = getCurrentUser();
        
        AuditLog auditLog = new AuditLog(
            entityType,
            entityId,
            action,
            oldValue,
            newValue,
            changedBy,
            employeeId,
            remarks
        );
        
        auditLogRepository.save(auditLog);
    }

    /**
     * Get current logged-in user
     */
    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    /**
     * Get audit logs for a specific employee
     */
    public java.util.List<AuditLog> getAuditLogsForEmployee(Long employeeId) {
        return auditLogRepository.findByEmployeeIdOrderByChangedAtDesc(employeeId);
    }

    /**
     * Get audit logs for a specific entity
     */
    public java.util.List<AuditLog> getAuditLogsForEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }
}
