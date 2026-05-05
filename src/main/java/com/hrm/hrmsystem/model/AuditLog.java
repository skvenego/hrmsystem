package com.hrm.hrmsystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityType; // "ATTENDANCE", "LEAVE", "PAYSLIP", etc.

    @Column(nullable = false)
    private Long entityId; // ID of the entity being audited

    @Column(nullable = false)
    private String action; // "CREATE", "UPDATE", "DELETE", "RESOLVE_PENDING", etc.

    @Column(nullable = false)
    private String oldValue; // JSON string of old value

    @Column(nullable = false)
    private String newValue; // JSON string of new value

    @Column(nullable = false)
    private String changedBy; // Username/email of who made the change

    @Column(nullable = false)
    private LocalDateTime changedAt;

    @Column(length = 500)
    private String remarks;

    @Column(nullable = false)
    private Long employeeId; // Employee affected by the change

    // Constructors
    public AuditLog() {
        this.changedAt = LocalDateTime.now();
    }

    public AuditLog(String entityType, Long entityId, String action, String oldValue, String newValue, String changedBy, Long employeeId, String remarks) {
        this();
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedBy = changedBy;
        this.employeeId = employeeId;
        this.remarks = remarks;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }
}
