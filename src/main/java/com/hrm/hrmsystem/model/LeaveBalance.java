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

    private Integer sickLeave;
    private Integer casualLeave;
    private Integer annualLeave;
    private Integer maternityLeave;
    private Integer paternityLeave;

    private Integer year;

    // Default Constructor
    public LeaveBalance() {}

    // All Args Constructor
    public LeaveBalance(Long id, Employee employee, Integer sickLeave, Integer casualLeave,
                        Integer annualLeave, Integer maternityLeave, Integer paternityLeave, Integer year) {
        this.id = id;
        this.employee = employee;
        this.sickLeave = sickLeave;
        this.casualLeave = casualLeave;
        this.annualLeave = annualLeave;
        this.maternityLeave = maternityLeave;
        this.paternityLeave = paternityLeave;
        this.year = year;
    }

    // Getters
    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public Integer getSickLeave() { return sickLeave; }
    public Integer getCasualLeave() { return casualLeave; }
    public Integer getAnnualLeave() { return annualLeave; }
    public Integer getMaternityLeave() { return maternityLeave; }
    public Integer getPaternityLeave() { return paternityLeave; }
    public Integer getYear() { return year; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public void setSickLeave(Integer sickLeave) { this.sickLeave = sickLeave; }
    public void setCasualLeave(Integer casualLeave) { this.casualLeave = casualLeave; }
    public void setAnnualLeave(Integer annualLeave) { this.annualLeave = annualLeave; }
    public void setMaternityLeave(Integer maternityLeave) { this.maternityLeave = maternityLeave; }
    public void setPaternityLeave(Integer paternityLeave) { this.paternityLeave = paternityLeave; }
    public void setYear(Integer year) { this.year = year; }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Employee employee;
        private Integer sickLeave;
        private Integer casualLeave;
        private Integer annualLeave;
        private Integer maternityLeave;
        private Integer paternityLeave;
        private Integer year;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder employee(Employee employee) { this.employee = employee; return this; }
        public Builder sickLeave(Integer sickLeave) { this.sickLeave = sickLeave; return this; }
        public Builder casualLeave(Integer casualLeave) { this.casualLeave = casualLeave; return this; }
        public Builder annualLeave(Integer annualLeave) { this.annualLeave = annualLeave; return this; }
        public Builder maternityLeave(Integer maternityLeave) { this.maternityLeave = maternityLeave; return this; }
        public Builder paternityLeave(Integer paternityLeave) { this.paternityLeave = paternityLeave; return this; }
        public Builder year(Integer year) { this.year = year; return this; }

        public LeaveBalance build() {
            return new LeaveBalance(id, employee, sickLeave, casualLeave, annualLeave, maternityLeave, paternityLeave, year);
        }
    }
}
