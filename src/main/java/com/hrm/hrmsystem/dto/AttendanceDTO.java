package com.hrm.hrmsystem.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class AttendanceDTO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDate date;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private Double workingHours;
    private Double overtimeHours;
    private String status;
    private String remarks;

    public AttendanceDTO() {}

    // Getters
    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public LocalDate getDate() { return date; }
    public LocalTime getCheckInTime() { return checkInTime; }
    public LocalTime getCheckOutTime() { return checkOutTime; }
    public Double getWorkingHours() { return workingHours; }
    public Double getOvertimeHours() { return overtimeHours; }
    public String getStatus() { return status; }
    public String getRemarks() { return remarks; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }
    public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public void setWorkingHours(Double workingHours) { this.workingHours = workingHours; }
    public void setOvertimeHours(Double overtimeHours) { this.overtimeHours = overtimeHours; }
    public void setStatus(String status) { this.status = status; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long employeeId;
        private String employeeName;
        private LocalDate date;
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private Double workingHours;
        private Double overtimeHours;
        private String status;
        private String remarks;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder employeeId(Long employeeId) { this.employeeId = employeeId; return this; }
        public Builder employeeName(String employeeName) { this.employeeName = employeeName; return this; }
        public Builder date(LocalDate date) { this.date = date; return this; }
        public Builder checkInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; return this; }
        public Builder checkOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; return this; }
        public Builder workingHours(Double workingHours) { this.workingHours = workingHours; return this; }
        public Builder overtimeHours(Double overtimeHours) { this.overtimeHours = overtimeHours; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder remarks(String remarks) { this.remarks = remarks; return this; }

        public AttendanceDTO build() {
            AttendanceDTO dto = new AttendanceDTO();
            dto.id = this.id;
            dto.employeeId = this.employeeId;
            dto.employeeName = this.employeeName;
            dto.date = this.date;
            dto.checkInTime = this.checkInTime;
            dto.checkOutTime = this.checkOutTime;
            dto.workingHours = this.workingHours;
            dto.overtimeHours = this.overtimeHours;
            dto.status = this.status;
            dto.remarks = this.remarks;
            return dto;
        }
    }
}
