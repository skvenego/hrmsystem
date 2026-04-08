package com.hrm.hrmsystem.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Employee Probation Period Tracking
 */
@Entity
@Table(name = "probation_periods")
public class ProbationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    /** Start date of probation */
    @Column(nullable = false)
    private LocalDate startDate;

    /** End date of probation */
    @Column(nullable = false)
    private LocalDate endDate;

    /** Duration in months */
    @Column(nullable = false)
    private Integer durationMonths;

    /** Status: PROBATION, CONFIRMED, EXTENDED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProbationStatus status = ProbationStatus.PROBATION;

    /** Is employee eligible for paid leaves? */
    @Column(nullable = false)
    private Boolean paidLeaveEligible = false;

    /** PF percentage (e.g., 12.0 for 12%) */
    @Column(nullable = false)
    private Double pfPercentage = 12.0;

    /** Annual tax amount */
    @Column(nullable = false)
    private Double annualTax = 0.0;

    /** Monthly TDS (calculated automatically) */
    @Column(nullable = false)
    private Double monthlyTds = 0.0;

    /** Extension notes if probation is extended */
    @Column(length = 500)
    private String extensionNotes;

    /** Confirmed by (HR/Manager) */
    @Column(length = 100)
    private String confirmedBy;

    /** Confirmation date */
    private LocalDate confirmedDate;

    @PrePersist
    protected void onCreate() {
        // Auto-calculate monthly TDS from annual tax
        if (this.annualTax != null && this.annualTax > 0) {
            this.monthlyTds = Math.round((this.annualTax / 12) * 100.0) / 100.0;
        }
        
        // Auto-calculate end date from start date and duration
        if (this.startDate != null && this.durationMonths != null) {
            this.endDate = startDate.plusMonths(durationMonths);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Recalculate monthly TDS if annual tax changes
        if (this.annualTax != null && this.annualTax > 0) {
            this.monthlyTds = Math.round((this.annualTax / 12) * 100.0) / 100.0;
        }
    }

    // Constructors
    public ProbationPeriod() {}

    public ProbationPeriod(Employee employee, LocalDate startDate, Integer durationMonths) {
        this.employee = employee;
        this.startDate = startDate;
        this.durationMonths = durationMonths;
        this.paidLeaveEligible = false; // Not eligible during probation
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { 
        this.startDate = startDate;
        if (this.durationMonths != null) {
            this.endDate = startDate.plusMonths(durationMonths);
        }
    }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public Integer getDurationMonths() { return durationMonths; }
    public void setDurationMonths(Integer durationMonths) { 
        this.durationMonths = durationMonths;
        if (this.startDate != null) {
            this.endDate = startDate.plusMonths(durationMonths);
        }
    }
    
    public ProbationStatus getStatus() { return status; }
    public void setStatus(ProbationStatus status) { 
        this.status = status;
        if (status == ProbationStatus.CONFIRMED) {
            this.paidLeaveEligible = true;
        }
    }
    
    public Boolean getPaidLeaveEligible() { return paidLeaveEligible; }
    public void setPaidLeaveEligible(Boolean paidLeaveEligible) { this.paidLeaveEligible = paidLeaveEligible; }
    
    public Double getPfPercentage() { return pfPercentage; }
    public void setPfPercentage(Double pfPercentage) { this.pfPercentage = pfPercentage; }
    
    public Double getAnnualTax() { return annualTax; }
    public void setAnnualTax(Double annualTax) { 
        this.annualTax = annualTax;
        // Auto-calculate monthly TDS
        if (annualTax != null && annualTax > 0) {
            this.monthlyTds = Math.round((annualTax / 12) * 100.0) / 100.0;
        }
    }
    
    public Double getMonthlyTds() { return monthlyTds; }
    public void setMonthlyTds(Double monthlyTds) { this.monthlyTds = monthlyTds; }
    
    public String getExtensionNotes() { return extensionNotes; }
    public void setExtensionNotes(String extensionNotes) { this.extensionNotes = extensionNotes; }
    
    public String getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(String confirmedBy) { this.confirmedBy = confirmedBy; }
    
    public LocalDate getConfirmedDate() { return confirmedDate; }
    public void setConfirmedDate(LocalDate confirmedDate) { this.confirmedDate = confirmedDate; }

    // Helper methods
    public boolean isProbationCompleted() {
        return LocalDate.now().isAfter(endDate) || status == ProbationStatus.CONFIRMED;
    }

    public long getRemainingProbationDays() {
        if (isProbationCompleted()) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), endDate);
    }

    // Enum for status
    public enum ProbationStatus {
        PROBATION,      // Currently in probation
        CONFIRMED,      // Probation completed and confirmed
        EXTENDED        // Probation extended
    }
}
