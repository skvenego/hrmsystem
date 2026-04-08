package com.hrm.hrmsystem.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Leave Balance with Carry Forward and Accrual
 */
@Entity
@Table(name = "leave_balances_enhanced")
public class LeaveBalanceEnhanced {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Year for which balance is tracked */
    @Column(nullable = false)
    private Integer year;

    /** Total earned leaves in the year (1.5 per month after probation) */
    @Column(nullable = false)
    private Double earnedLeaves = 0.0;

    /** Total available leaves (earned + carried forward) */
    @Column(nullable = false)
    private Double totalAvailable = 0.0;

    /** Leaves used/availed */
    @Column(nullable = false)
    private Double usedLeaves = 0.0;

    /** Remaining balance */
    @Column(nullable = false)
    private Double remainingLeaves = 0.0;

    /** Leaves carried forward from previous year */
    @Column(nullable = false)
    private Double carriedForward = 0.0;

    /** Leaves lapsed (if any policy exists) */
    @Column(nullable = false)
    private Double lapsedLeaves = 0.0;

    /** Last updated timestamp */
    @Column(nullable = false)
    private LocalDate lastUpdated = LocalDate.now();

    @OneToMany(mappedBy = "leaveBalance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeaveTransaction> transactions = new ArrayList<>();

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDate.now();
        // Auto-calculate remaining
        this.remainingLeaves = this.totalAvailable - this.usedLeaves;
    }

    // Constructors
    public LeaveBalanceEnhanced() {}

    public LeaveBalanceEnhanced(Employee employee, Integer year) {
        this.employee = employee;
        this.year = year;
        this.earnedLeaves = 0.0;
        this.totalAvailable = 0.0;
        this.usedLeaves = 0.0;
        this.remainingLeaves = 0.0;
        this.carriedForward = 0.0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    
    public Double getEarnedLeaves() { return earnedLeaves; }
    public void setEarnedLeaves(Double earnedLeaves) { 
        this.earnedLeaves = earnedLeaves;
        this.totalAvailable = this.earnedLeaves + this.carriedForward;
    }
    
    public Double getTotalAvailable() { return totalAvailable; }
    public void setTotalAvailable(Double totalAvailable) { this.totalAvailable = totalAvailable; }
    
    public Double getUsedLeaves() { return usedLeaves; }
    public void setUsedLeaves(Double usedLeaves) { this.usedLeaves = usedLeaves; }
    
    public Double getRemainingLeaves() { return remainingLeaves; }
    public void setRemainingLeaves(Double remainingLeaves) { this.remainingLeaves = remainingLeaves; }
    
    public Double getCarriedForward() { return carriedForward; }
    public void setCarriedForward(Double carriedForward) { 
        this.carriedForward = carriedForward;
        this.totalAvailable = this.earnedLeaves + this.carriedForward;
    }
    
    public Double getLapsedLeaves() { return lapsedLeaves; }
    public void setLapsedLeaves(Double lapsedLeaves) { this.lapsedLeaves = lapsedLeaves; }
    
    public LocalDate getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDate lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public List<LeaveTransaction> getTransactions() { return transactions; }
    public void setTransactions(List<LeaveTransaction> transactions) { this.transactions = transactions; }

    // Helper methods
    public void addUsedLeave(Double days) {
        if (days == null) days = 0.0;
        this.usedLeaves += days;
        this.remainingLeaves = this.totalAvailable - this.usedLeaves;
    }

    public void addEarnedLeave(Double days) {
        if (days == null) days = 0.0;
        this.earnedLeaves += days;
        this.totalAvailable = this.earnedLeaves + this.carriedForward;
        this.remainingLeaves = this.totalAvailable - this.usedLeaves;
    }

    public boolean hasSufficientBalance(Double requestedDays) {
        return this.remainingLeaves >= (requestedDays != null ? requestedDays : 0.0);
    }
}
