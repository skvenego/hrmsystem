package com.hrm.hrmsystem.dto;

import java.time.LocalDate;

public class LeaveDTO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private Integer paidDays;
    private Integer unpaidDays;
    private Boolean isHalfDay;
    private String reason;
    private String status;
    private String approvedBy;
    private String rejectionReason;
    private LocalDate appliedDate;

    public LeaveDTO() {}

    // Getters
    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getLeaveType() { return leaveType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getTotalDays() { return totalDays; }
    public Integer getPaidDays() { return paidDays; }
    public Integer getUnpaidDays() { return unpaidDays; }
    public Boolean getIsHalfDay() { return isHalfDay; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public String getApprovedBy() { return approvedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDate getAppliedDate() { return appliedDate; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setTotalDays(Integer totalDays) { this.totalDays = totalDays; }
    public void setPaidDays(Integer paidDays) { this.paidDays = paidDays; }
    public void setUnpaidDays(Integer unpaidDays) { this.unpaidDays = unpaidDays; }
    public void setIsHalfDay(Boolean isHalfDay) { this.isHalfDay = isHalfDay; }
    public void setReason(String reason) { this.reason = reason; }
    public void setStatus(String status) { this.status = status; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public void setAppliedDate(LocalDate appliedDate) { this.appliedDate = appliedDate; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long employeeId;
        private String employeeName;
        private String leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer totalDays;
        private Integer paidDays;
        private Integer unpaidDays;
        private Boolean isHalfDay;
        private String reason;
        private String status;
        private String approvedBy;
        private String rejectionReason;
        private LocalDate appliedDate;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder employeeId(Long employeeId) { this.employeeId = employeeId; return this; }
        public Builder employeeName(String employeeName) { this.employeeName = employeeName; return this; }
        public Builder leaveType(String leaveType) { this.leaveType = leaveType; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder totalDays(Integer totalDays) { this.totalDays = totalDays; return this; }
        public Builder paidDays(Integer paidDays) { this.paidDays = paidDays; return this; }
        public Builder unpaidDays(Integer unpaidDays) { this.unpaidDays = unpaidDays; return this; }
        public Builder isHalfDay(Boolean isHalfDay) { this.isHalfDay = isHalfDay; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder approvedBy(String approvedBy) { this.approvedBy = approvedBy; return this; }
        public Builder rejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; return this; }
        public Builder appliedDate(LocalDate appliedDate) { this.appliedDate = appliedDate; return this; }

        public LeaveDTO build() {
            LeaveDTO dto = new LeaveDTO();
            dto.id = this.id;
            dto.employeeId = this.employeeId;
            dto.employeeName = this.employeeName;
            dto.leaveType = this.leaveType;
            dto.startDate = this.startDate;
            dto.endDate = this.endDate;
            dto.totalDays = this.totalDays;
            dto.paidDays = this.paidDays;
            dto.unpaidDays = this.unpaidDays;
            dto.isHalfDay = this.isHalfDay;
            dto.reason = this.reason;
            dto.status = this.status;
            dto.approvedBy = this.approvedBy;
            dto.rejectionReason = this.rejectionReason;
            dto.appliedDate = this.appliedDate;
            return dto;
        }
    }
}
