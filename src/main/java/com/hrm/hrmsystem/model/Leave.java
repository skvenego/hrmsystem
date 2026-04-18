package com.hrm.hrmsystem.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "leaves")
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveType leaveType;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days")
    private Double totalDays;

    @Column(name = "paid_days")
    private Double paidDays;

    @Column(name = "unpaid_days")
    private Double unpaidDays;
    
    private Boolean isHalfDay = false;

    private String reason;

    @Enumerated(EnumType.STRING)
    private LeaveStatus status;

    private String approvedBy;

    private String rejectionReason;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    private LocalDate appliedDate;

    public enum LeaveType {
        SICK, CASUAL, ANNUAL, MATERNITY, PATERNITY, UNPAID, HALF
    }

    public enum LeaveStatus {
        PENDING, APPROVED, REJECTED, CANCELLED
    }

    // Default Constructor
    public Leave() {}

    // All Args Constructor
    public Leave(Long id, Employee employee, LeaveType leaveType, LocalDate startDate,
                 LocalDate endDate, Double totalDays, Double paidDays, Double unpaidDays,
                 Boolean isHalfDay, String reason, LeaveStatus status,
                 String approvedBy, String rejectionReason, String contactNumber, LocalDate appliedDate) {
        this.id = id;
        this.employee = employee;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalDays = totalDays;
        this.paidDays = paidDays;
        this.unpaidDays = unpaidDays;
        this.isHalfDay = isHalfDay;
        this.reason = reason;
        this.status = status;
        this.approvedBy = approvedBy;
        this.rejectionReason = rejectionReason;
        this.contactNumber = contactNumber;
        this.appliedDate = appliedDate;
    }

    // Getters
    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public LeaveType getLeaveType() { return leaveType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Double getTotalDays() { return totalDays; }
    public Double getPaidDays() { return paidDays; }
    public Double getUnpaidDays() { return unpaidDays; }
    public Boolean getIsHalfDay() { return isHalfDay; }
    public String getReason() { return reason; }
    public LeaveStatus getStatus() { return status; }
    public String getApprovedBy() { return approvedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public String getContactNumber() { return contactNumber; }
    public LocalDate getAppliedDate() { return appliedDate; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setTotalDays(Double totalDays) { this.totalDays = totalDays; }
    public void setPaidDays(Double paidDays) { this.paidDays = paidDays; }
    public void setUnpaidDays(Double unpaidDays) { this.unpaidDays = unpaidDays; }
    public void setIsHalfDay(Boolean isHalfDay) { this.isHalfDay = isHalfDay; }
    public void setReason(String reason) { this.reason = reason; }
    public void setStatus(LeaveStatus status) { this.status = status; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public void setAppliedDate(LocalDate appliedDate) { this.appliedDate = appliedDate; }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Employee employee;
        private LeaveType leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Double totalDays;
        private Double paidDays;
        private Double unpaidDays;
        private Boolean isHalfDay = false;
        private String reason;
        private LeaveStatus status;
        private String approvedBy;
        private String rejectionReason;
        private String contactNumber;
        private LocalDate appliedDate;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder employee(Employee employee) { this.employee = employee; return this; }
        public Builder leaveType(LeaveType leaveType) { this.leaveType = leaveType; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder totalDays(Double totalDays) { this.totalDays = totalDays; return this; }
        public Builder paidDays(Double paidDays) { this.paidDays = paidDays; return this; }
        public Builder unpaidDays(Double unpaidDays) { this.unpaidDays = unpaidDays; return this; }
        public Builder isHalfDay(Boolean isHalfDay) { this.isHalfDay = isHalfDay; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder status(LeaveStatus status) { this.status = status; return this; }
        public Builder approvedBy(String approvedBy) { this.approvedBy = approvedBy; return this; }
        public Builder rejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; return this; }
        public Builder contactNumber(String contactNumber) { this.contactNumber = contactNumber; return this; }
        public Builder appliedDate(LocalDate appliedDate) { this.appliedDate = appliedDate; return this; }

        public Leave build() {
            return new Leave(id, employee, leaveType, startDate, endDate, totalDays, paidDays, unpaidDays, isHalfDay, reason, status, approvedBy, rejectionReason, contactNumber, appliedDate);
        }
    }
}
