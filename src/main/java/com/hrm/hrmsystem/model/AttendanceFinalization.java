package com.hrm.hrmsystem.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_finalizations")
public class AttendanceFinalization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalDate finalizationDate;

    @Column(nullable = false)
    private LocalDateTime finalizedAt;

    @Column(nullable = false)
    private String finalizedBy; // Username/email of who finalized attendance

    @Column(length = 500)
    private String remarks;

    @Column(nullable = false)
    private Boolean isFinalized = true;

    // Constructors
    public AttendanceFinalization() {
        this.finalizedAt = LocalDateTime.now();
        this.isFinalized = true;
    }

    public AttendanceFinalization(LocalDate startDate, LocalDate endDate, String finalizedBy, String remarks) {
        this();
        this.startDate = startDate;
        this.endDate = endDate;
        this.finalizationDate = LocalDate.now();
        this.finalizedBy = finalizedBy;
        this.remarks = remarks;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getFinalizationDate() {
        return finalizationDate;
    }

    public void setFinalizationDate(LocalDate finalizationDate) {
        this.finalizationDate = finalizationDate;
    }

    public LocalDateTime getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(LocalDateTime finalizedAt) {
        this.finalizedAt = finalizedAt;
    }

    public String getFinalizedBy() {
        return finalizedBy;
    }

    public void setFinalizedBy(String finalizedBy) {
        this.finalizedBy = finalizedBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Boolean getIsFinalized() {
        return isFinalized;
    }

    public void setIsFinalized(Boolean isFinalized) {
        this.isFinalized = isFinalized;
    }
}
