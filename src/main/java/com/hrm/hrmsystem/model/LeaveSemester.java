package com.hrm.hrmsystem.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Leave Semester - Tracks leaves per semester (Jan-Jun, Jul-Dec)
 */
@Entity
@Table(name = "leave_semesters")
public class LeaveSemester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Year (e.g., 2026) */
    @Column(nullable = false)
    private Integer year;

    /** Semester: S1 (Jan-Jun) or S2 (Jul-Dec) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SemesterType semester;

    /** Start date of semester */
    @Column(nullable = false)
    private LocalDate startDate;

    /** End date of semester */
    @Column(nullable = false)
    private LocalDate endDate;

    /** Expiry date (end of semester) */
    @Column(nullable = false)
    private LocalDate expiryDate;

    /** Total leaves earned in this semester (1.5 leaves × months) */
    @Column(nullable = false)
    private Double earnedLeaves = 0.0;

    /** Leaves used/availed */
    @Column(nullable = false)
    private Double usedLeaves = 0.0;

    /** Remaining balance */
    @Column(nullable = false)
    private Double remainingLeaves = 0.0;

    /** Leaves carried forward to next semester/year */
    @Column(nullable = false)
    private Double carriedForward = 0.0;

    /** Leaves lapsed/expired */
    @Column(nullable = false)
    private Double lapsedLeaves = 0.0;

    /** Is this semester active? */
    @Column(nullable = false)
    private Boolean isActive = false;

    /** Last updated */
    @Column(nullable = false)
    private LocalDate lastUpdated = LocalDate.now();

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDate.now();
        // Auto-calculate remaining
        this.remainingLeaves = this.earnedLeaves + this.carriedForward - this.usedLeaves;
        
        // Check if semester is expired
        if (LocalDate.now().isAfter(expiryDate)) {
            this.isActive = false;
        }
    }

    // Constructors
    public LeaveSemester() {}

    public LeaveSemester(Employee employee, Integer year, SemesterType semester, 
                         LocalDate startDate, LocalDate endDate) {
        this.employee = employee;
        this.year = year;
        this.semester = semester;
        this.startDate = startDate;
        this.endDate = endDate;
        this.expiryDate = endDate;
        this.isActive = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    
    public SemesterType getSemester() { return semester; }
    public void setSemester(SemesterType semester) { this.semester = semester; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    
    public Double getEarnedLeaves() { return earnedLeaves; }
    public void setEarnedLeaves(Double earnedLeaves) { this.earnedLeaves = earnedLeaves; }
    
    public Double getUsedLeaves() { return usedLeaves; }
    public void setUsedLeaves(Double usedLeaves) { this.usedLeaves = usedLeaves; }
    
    public Double getRemainingLeaves() { return remainingLeaves; }
    public void setRemainingLeaves(Double remainingLeaves) { this.remainingLeaves = remainingLeaves; }
    
    public Double getCarriedForward() { return carriedForward; }
    public void setCarriedForward(Double carriedForward) { this.carriedForward = carriedForward; }
    
    public Double getLapsedLeaves() { return lapsedLeaves; }
    public void setLapsedLeaves(Double lapsedLeaves) { this.lapsedLeaves = lapsedLeaves; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDate getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDate lastUpdated) { this.lastUpdated = lastUpdated; }

    // Helper methods
    public long getDaysUntilExpiry() {
        if (LocalDate.now().isAfter(expiryDate)) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    public String getStatusDisplay() {
        if (isExpired()) {
            return "EXPIRED on " + expiryDate;
        } else if (getDaysUntilExpiry() <= 15) {
            return "⚠️ Expires in " + getDaysUntilExpiry() + " days";
        } else {
            return "Valid until " + expiryDate;
        }
    }

    // Enum for semester types
    public enum SemesterType {
        S1("January - June"),   // First half: Jan to Jun
        S2("July - December");  // Second half: Jul to Dec
        
        private final String displayName;
        
        SemesterType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static SemesterType fromMonth(int month) {
            if (month >= 1 && month <= 6) {
                return S1;
            } else {
                return S2;
            }
        }
    }
}
