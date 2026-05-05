package com.hrm.hrmsystem.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll_locks")
public class PayrollLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private LocalDate lockDate;

    @Column(nullable = false)
    private LocalDateTime lockedAt;

    @Column(nullable = false)
    private String lockedBy; // Username/email of who locked the payroll

    @Column(length = 500)
    private String remarks;

    @Column(nullable = false)
    private Boolean isLocked = true;

    // Constructors
    public PayrollLock() {
        this.lockedAt = LocalDateTime.now();
        this.isLocked = true;
    }

    public PayrollLock(Integer month, Integer year, String lockedBy, String remarks) {
        this();
        this.month = month;
        this.year = year;
        this.lockDate = LocalDate.now();
        this.lockedBy = lockedBy;
        this.remarks = remarks;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public LocalDate getLockDate() {
        return lockDate;
    }

    public void setLockDate(LocalDate lockDate) {
        this.lockDate = lockDate;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Boolean getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }
}
