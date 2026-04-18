package com.hrm.hrmsystem.model;

import jakarta.persistence.*;

@Entity
@Table(name = "leave_balances")
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // Simplified paid leave balance system (1.5 leaves per month)
    private Integer totalEarnedLeaves;
    private Double usedLeaves;
    private Double unpaidLeaves;
    private Integer carriedForwardLeaves;
    
    // Cycle tracking (Jan-Jun, Jul-Dec)
    private Integer cycle; // 1 or 2
    private Integer year;

    // Default Constructor
    public LeaveBalance() {}

    // All Args Constructor
    public LeaveBalance(Long id, Employee employee, Integer totalEarnedLeaves, Double usedLeaves,
                        Double unpaidLeaves, Integer carriedForwardLeaves, Integer cycle, Integer year) {
        this.id = id;
        this.employee = employee;
        this.totalEarnedLeaves = totalEarnedLeaves;
        this.usedLeaves = usedLeaves;
        this.unpaidLeaves = unpaidLeaves;
        this.carriedForwardLeaves = carriedForwardLeaves;
        this.cycle = cycle;
        this.year = year;
    }

    // Getters
    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public Integer getTotalEarnedLeaves() { return totalEarnedLeaves; }
    public Double getUsedLeaves() { return usedLeaves; }
    public Double getUnpaidLeaves() { return unpaidLeaves; }
    public Integer getCarriedForwardLeaves() { return carriedForwardLeaves; }
    public Integer getCycle() { return cycle; }
    public Integer getYear() { return year; }
    
    // Helper methods
    public Double getAvailableLeaves() {
        return (totalEarnedLeaves != null ? totalEarnedLeaves.doubleValue() : 0.0) + 
               (carriedForwardLeaves != null ? carriedForwardLeaves.doubleValue() : 0.0) - 
               (usedLeaves != null ? usedLeaves : 0.0);
    }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public void setTotalEarnedLeaves(Integer totalEarnedLeaves) { this.totalEarnedLeaves = totalEarnedLeaves; }
    public void setUsedLeaves(Double usedLeaves) { this.usedLeaves = usedLeaves; }
    public void setUnpaidLeaves(Double unpaidLeaves) { this.unpaidLeaves = unpaidLeaves; }
    public void setCarriedForwardLeaves(Integer carriedForwardLeaves) { this.carriedForwardLeaves = carriedForwardLeaves; }
    public void setCycle(Integer cycle) { this.cycle = cycle; }
    public void setYear(Integer year) { this.year = year; }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Employee employee;
        private Integer totalEarnedLeaves;
        private Double usedLeaves;
        private Double unpaidLeaves;
        private Integer carriedForwardLeaves;
        private Integer cycle;
        private Integer year;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder employee(Employee employee) { this.employee = employee; return this; }
        public Builder totalEarnedLeaves(Integer totalEarnedLeaves) { this.totalEarnedLeaves = totalEarnedLeaves; return this; }
        public Builder usedLeaves(Double usedLeaves) { this.usedLeaves = usedLeaves; return this; }
        public Builder unpaidLeaves(Double unpaidLeaves) { this.unpaidLeaves = unpaidLeaves; return this; }
        public Builder carriedForwardLeaves(Integer carriedForwardLeaves) { this.carriedForwardLeaves = carriedForwardLeaves; return this; }
        public Builder cycle(Integer cycle) { this.cycle = cycle; return this; }
        public Builder year(Integer year) { this.year = year; return this; }

        public LeaveBalance build() {
            return new LeaveBalance(id, employee, totalEarnedLeaves, usedLeaves, unpaidLeaves, carriedForwardLeaves, cycle, year);
        }
    }
}
